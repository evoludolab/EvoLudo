#!/usr/bin/env node

//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

//
// Feed the command line options of the EvoLudo test generators to the GUI
// running in a browser and save the resulting states. Driven by playwright,
// launched from scripts/runGUItests.sh, which also takes care of comparing
// the exported states to the reference files.
//
// For every option set the browser loads TestEvoLudo.html with the options
// appended to the URL. In addition to '--seed 0' and '--delay 0' (as in the
// JRE test suite) the options '--run' and '--snap' are added: '--snap' marks
// the end of the run by adding a '<div id="snapshot-ready">' to the DOM and
// derives whether the model needs to run from the requested end state (see
// EvoLudoGWT#cloSnap). Once the marker shows up, 'Shift-E' exports the state,
// which is intercepted (rather than downloaded) and written to disk.
//

const fs = require('fs');
const path = require('path');

const { chromium } = require('playwright');

/**
 * Print the help screen.
 */
function printHelp() {
	console.log('Usage: guitests.js [options] TEST.clo ...');
	console.log('');
	console.log('Options:');
	console.log('  --url <url>          base URL of the EvoLudo development server');
	console.log('  --exports <dir>      directory for the states exported by the GUI');
	console.log('  --references <dir>   directory with the reference test cases');
	console.log('  --timeout <s>        timeout per test case (default 300s)');
	console.log('  --snap <s>           timeout for laying out networks (default 20s)');
	console.log('  --view <v>           initial view for every test, see EvoLudoWeb --view');
	console.log('  --headful, --headed  show the browser window');
	console.log('  --verb               verbose mode, echo the browser console');
	console.log('  -h, --help           show this help screen');
}

/**
 * Parse the command line arguments.
 *
 * @return {object} the configuration for this run
 */
function parse(argv) {
	const config = {
		url: 'http://localhost:8888',
		exports: 'EvoLudoTest/tests/exports',
		references: 'EvoLudoTest/tests/references',
		timeout: 300,
		snap: 20,
		view: null,
		headed: false,
		verbose: false,
		tests: []
	};
	for (let i = 0; i < argv.length; i++) {
		const arg = argv[i];
		switch (arg) {
			case '-h':
			case '--help':
				printHelp();
				process.exit(0);
				break;
			case '--url':
				config.url = argv[++i];
				break;
			case '--exports':
				config.exports = argv[++i];
				break;
			case '--references':
				config.references = argv[++i];
				break;
			case '--timeout':
				config.timeout = Number(argv[++i]);
				break;
			case '--snap':
				config.snap = Number(argv[++i]);
				break;
			case '--view':
				config.view = argv[++i];
				break;
			case '--headed':
			case '--headful':
				config.headed = true;
				break;
			case '--verb':
				config.verbose = true;
				break;
			default:
				if (arg.startsWith('-')) {
					console.error(`unknown option '${arg}' - aborting!`);
					process.exit(1);
				}
				config.tests.push(arg);
		}
	}
	if (config.tests.length === 0) {
		printHelp();
		process.exit(1);
	}
	return config;
}

/**
 * Read the option sets from the generator file 'clofile'. Comments and empty
 * lines are skipped but the remaining lines are numbered as in the JRE test
 * suite, see TestEvoLudo#generate(File).
 *
 * @param {string} clofile the generator file
 * @return {string[]} the option sets
 */
function readGenerator(clofile) {
	return fs.readFileSync(clofile, 'utf8')
		.split('\n')
		.map((line) => line.trim())
		.filter((line) => line.length > 0 && !line.startsWith('#'));
}

/**
 * Strip the '--export' option from the command line options 'clo'. The option
 * is meaningless in the browser (and discarded by EvoLudoGWT) but its argument
 * determines the name of the reference file, see {@link referenceName}.
 *
 * @param {string} clo the command line options
 * @return {string} the stripped command line options
 */
function stripExport(clo) {
	let idx;
	while ((idx = clo.indexOf('--export')) >= 0) {
		const end = clo.indexOf('--', idx + '--export'.length);
		clo = (clo.substring(0, idx) + (end < 0 ? '' : clo.substring(end))).trim();
	}
	return clo;
}

/**
 * Map the indices of the test cases to the names of their reference files in
 * directory 'refdir'. Reference files are named '<module>-<idx>-<model>-SHA...'
 * unless the option set requested a name through '--export', see
 * TestEvoLudo#generateBasename(String, int).
 *
 * @param {string} refdir the directory with the reference files
 * @return {Map<number, string>} the reference names by index
 */
function readReferences(refdir) {
	const names = new Map();
	let entries;
	try {
		entries = fs.readdirSync(refdir);
	} catch (err) {
		return names;
	}
	for (const entry of entries) {
		const sha = entry.indexOf('-SHA');
		if (sha < 0)
			continue;
		const name = entry.substring(0, sha);
		// note: model keys do not contain '-', hence the index is the number
		// preceding the last '-' separated component
		const idx = name.match(/-(\d+)-[^-]*$/);
		if (idx)
			names.set(Number(idx[1]), name);
	}
	return names;
}

/**
 * Determine the name of the reference file for the option set 'clo' with index
 * 'idx'. Mirrors TestEvoLudo#generateBasename(String, int) but reads the module
 * and model keys from the names of the reference files rather than from the
 * engine.
 *
 * @param {string} clo  the command line options
 * @param {number} idx  the index of the test case
 * @param {Map<number, string>} refs the reference names by index
 * @return {string|null} the name of the reference or 'null' if not found
 */
function referenceName(clo, idx, refs) {
	const exp = clo.match(/--export\s+(\S+)/);
	if (exp)
		return exp[1].replace('.plist', '').replace('.zip', '');
	return refs.has(idx) ? refs.get(idx) : null;
}

/**
 * Intercept the download of the state that EvoLudo triggers in the browser and
 * stash it in 'window.__evoludoExport' instead, see NativeJS#export(String,
 * String). Installed before any EvoLudo code runs.
 */
function interceptExport() {
	window.__evoludoExport = null;
	document.addEventListener('click', (event) => {
		const target = event.target;
		const anchor = target && target.closest ? target.closest('a[download]') : null;
		if (!anchor)
			return;
		window.__evoludoExport = anchor.getAttribute('href');
		// suppress the download, the state is picked up from the page
		event.preventDefault();
		event.stopPropagation();
	}, true);
}

/**
 * Run the option set 'clo' in the browser and return the exported state.
 *
 * @param {object} page   the playwright page
 * @param {string} clo    the command line options
 * @param {object} config the configuration for this run
 * @return {Promise<string>} the state exported by the GUI
 */
async function runTest(page, clo, config) {
	// mirror the JRE test suite: fixed seed, full speed, no export to file;
	// '--snap' signals when the requested end state has been reached. '--view'
	// is prepended, not appended, so that later options take precedence, see
	// TestEvoLudo#runModule(String, String): a test whose own generator line
	// sets '--view' is free to override this run's default view.
	config.errors.length = 0;
	const view = config.view ? `--view ${config.view} ` : '';
	const options = `--seed 0 ${view}${stripExport(clo)} --delay 0 --run --snap ${config.snap}`;
	const url = `${config.url}/TestEvoLudo.html?clo=${encodeURIComponent(options)}`;
	const timeout = config.timeout * 1000;
	await page.goto(url, { timeout });
	// the lab reports fatal internal errors through the second marker, see
	// EvoLudoWeb#snapshotFailed(String), otherwise the wait would block
	await page.waitForSelector('#snapshot-ready, #snapshot-failed', { timeout, state: 'attached' });
	const failed = await page.evaluate(() => {
		const marker = document.getElementById('snapshot-failed');
		return marker ? marker.textContent || 'internal error in the GUI' : null;
	});
	if (failed)
		throw new Error(failed);
	// make sure keyboard shortcuts are not swallowed by the settings field
	await page.evaluate(() => {
		const active = document.activeElement;
		if (active && active !== document.body && active.blur)
			active.blur();
		window.__evoludoExport = null;
	});
	// 'Shift-E' exports the state of the model, see KeyHandler#handleExport()
	await page.keyboard.press('Shift+E');
	const handle = await page.waitForFunction(() => window.__evoludoExport, null, { timeout });
	const href = await handle.jsonValue();
	const b64 = href.substring(href.indexOf(',') + 1);
	// the state is encoded with btoa, i.e. one byte per character
	return Buffer.from(b64, 'base64').toString('latin1');
}

/**
 * Feed the option sets in the generator file 'clofile' to the GUI in the
 * browser and save the exported states in the exports directory.
 *
 * @param {object} page    the playwright page
 * @param {string} clofile the generator file
 * @param {object} config  the configuration for this run
 * @return {Promise<number>} the number of test cases that failed to export
 */
async function runGenerator(page, clofile, config) {
	const name = path.basename(clofile, '.clo');
	const refs = readReferences(path.join(config.references, name));
	const exports = path.join(config.exports, name);
	fs.mkdirSync(exports, { recursive: true });
	// discard the exports of a previous run to prevent stale states from
	// sneaking into the comparison with the references
	for (const entry of fs.readdirSync(exports)) {
		if (entry.endsWith('.plist'))
			fs.unlinkSync(path.join(exports, entry));
	}
	const clos = readGenerator(clofile);
	let failures = 0;
	for (let idx = 0; idx < clos.length; idx++) {
		const clo = clos[idx];
		const test = referenceName(clo, idx + 1, refs) || `${name}-${idx + 1}-unknown`;
		try {
			const state = await runTest(page, clo, config);
			// write the bytes as they were encoded in the browser, see runTest
			fs.writeFileSync(path.join(exports, `${test}.plist`), state, 'latin1');
			console.log(`Exported: ${name}${path.sep}${test}`);
		} catch (err) {
			failures++;
			console.error(`ERROR: ${name}${path.sep}${test} failed to export state - review!`);
			console.error(`       options '${clo}'`);
			console.error(`       ${err.message.split('\n')[0]}`);
			for (const error of config.errors)
				console.error(`       [browser] ${error}`);
		}
	}
	return failures;
}

/**
 * Entry point for feeding the EvoLudo tests to the GUI in the browser.
 */
async function main() {
	const config = parse(process.argv.slice(2));
	const browser = await chromium.launch({ headless: !config.headed });
	const page = await browser.newPage();
	await page.addInitScript(interceptExport);
	// errors that escape the lab, in particular in native javascript; the lab
	// reports the errors it catches itself through the 'snapshot-failed' marker
	config.errors = [];
	page.on('pageerror', (err) => {
		config.errors.push(err.message.split('\n')[0]);
		if (config.verbose)
			console.error(`  [browser] ${err.message}`);
	});
	if (config.verbose)
		page.on('console', (msg) => console.log(`  [browser] ${msg.text()}`));
	let failures = 0;
	try {
		for (const clofile of config.tests)
			failures += await runGenerator(page, clofile, config);
	} finally {
		await browser.close();
	}
	if (failures > 0)
		console.error(`ERROR: ${failures} test case(s) failed to export a state in the browser.`);
	process.exit(failures > 0 ? 1 : 0);
}

main();
