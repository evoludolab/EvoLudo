# Changelog

All notable project revisions are listed here in reverse chronological order.

## [`v2.2.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.2.1) (2026-03-14) Public release: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14591549.svg)](https://zenodo.org/doi/10.5281/zenodo.14591549)

### Bugfixes:
- Improved geometry and network robustness with safer allocation/clearing (`b4de6af03`), fixes for random/network geometry memory handling (`63395a3c7`), and corrected Barabasi-Albert connectivity handling (`92c3ed958`).
- Hardened statistics and run-mode handling by preserving model state on apply (`caf9cc8fe`), stopping runs on mode changes (`1102c1c2a`), reporting failed samples immediately (`0d71bc142`), and fixing running-statistics edge cases (`fee01150f`).

### Backend changes:
- Refined model and engine behavior by delegating default model creation to the engine (`03c70227c`), adding `SDEN` to model creation (`72fa74cc4`), and fixing custom-model setup and related semantics (`14be5f0c8`).
- Extended geometry semantics with geometry similarity/equality helpers (`12cf34d80`) and layout triggering only on geometry changes (`a96f4241f`).
- Continued backend model work by publishing the `Mutualism` module (`524f0a29b`) and tightening related model behavior (`9b78bdaec`).

### GUI changes:
- Made view activation and reset behavior more robust by improving 3D loading (`0545e4c3d`), deactivating the active view during GUI reconfiguration (`f31e70da0`), and cancelling layouting on reset (`3c880dd1e`).
- Improved browser responsiveness and user feedback during long operations by adding busy/fatal overlays (`5fa7706ae`, `534dae8aa`), reworking chunked scheduling (`0236769b0`), and refining progress reporting (`2ce763bba`).
- Expanded and polished graph/view controls with legends and positioning options (`968cdced0`, `93e8e9202`), improved context menus (`6a124fe37`), histogram/distribution refinements (`de3a25728`), and better y-axis label placement (`e57bf4668`).

## [`v2.2.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.2.0) (2026-02-18) Public release: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14591549.svg)](https://zenodo.org/doi/10.5281/zenodo.14591549)

- Upgraded GWT to `2.13` (`bd73624fa`), added Maven repo packaging (`922d7f5e9`), and improved publish scripting (`cb6f86bd1`).
- Fixed the delay default (`33fd9b6bf`), refreshed README docs (`f75725aea`), and added missing copyright headers (`4cef9bdf8`).
- Refined hierarchy/geometry integration (`2f405ce22`), consolidated lattice-dimension checks (`a2ab958fc`), and fixed second-neighbour boundary handling (`feff5aa95`).
- Streamlined GWT view-update throttling (`882d422a8`), improved histogram vertical sizing (`23f6722ee`), and fixed S3 initial-state handling (`0d33005ba`).
- Improved graph GUI controls: added `ParaGraph` zoom/shift interactions (`b5c6d19c3`) and enabled y-axis label placement to switch between left and right (`2457cda37`).
- Streamlined the context menu, including dedicated zoom and axis submenus (`7d972a36a`, `522f7e77e`).
- Refactored `EvoLudoWeb` view control (`cbe84221a`), extracted `CLOController` (`ffecb236b`), and extracted `RunController` (`323577b11`).
- Introduced major geometry architecture updates: new geometry package/class rollout (`a38ec4120`), hierarchical-geometry rework (`1935d000d`), and geom-package cleanup/renaming (`9888198b5`).
- Expanded multi-species ecological updates in `LV` (`3ab27e2ee`), added DE cross-species competition (`bf1dfc476`), and improved `ParaGraph` autoscaling/clamping for `LV` (`3408de599`).
- Hardened module/model load-unload and CLO handling: model load flow updates (`e284ea4b6`), provider-aware model (un)load (`412d982cf`), and stricter CLO parser behavior (`1de37827f`).

## [`v2.1.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.1.1) (2025-07-28)

- Added a null-safety guard for `activeModel` (`3fb712564`) and updated README documentation links (`065c6bcfe`).

## [`v2.1.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.1.0) (2025-07-28)

- Added the `LV` module (`cea81d7c4`), fixed SDE behavior for multi-species/frequency dynamics (`9df4761d2`), and added dual DE dynamics support (`3a0b86c01`).
- Improved realtime increment handling (`c4dd004bc`), refreshed tests for realtime changes (`e624941e7`), and fixed SDE statistics reporting (`d7699fb80`).
- Refactored model interfaces around DE/payoff semantics: `Scores` to `Payoffs` rename (`d74f7805f`), `HasDE` API tightening (`612da3497`), and DE-dependent/multispecies abstraction changes (`9b3591ab8`).
- Continued SIR/ecological model improvements: SIR module introduction (`2270e4e71`), improved optional transition defaults (`ff0c14868`), and follow-up SIR cleanup (`b8f49d343`).

## [`v2.0.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.2) (2025-03-09)

- Consolidated javadoc work (`773bd0e28`), added DOI/badge updates in README (`bf58ecec8`), and improved test generation scripting (`850099971`).

## [`v2.0.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.1) (2025-01-31)

- Fixed javadoc issues (`b03cf4874`), ensured IBS memory allocation in `check()` (`5f6e8fa93`), adjusted listener ordering (`0b4df073d`), and improved statistics checks (`9ac55a3b9`).

## [`v2.0.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.0) (2025-01-03) Initial public release: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14591549.svg)](https://zenodo.org/doi/10.5281/zenodo.14591549)

- Removed unnecessary population load/unload churn (`c45b71449`), fixed CLO-related testing issues (`59d3a0de3`), added GWT `--help` support (`59ee1de90`), and updated continuous-mutation naming (`e9dfaedb4`).

## [`v1.4.11`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.11) (2024-12-29)

- Test/script refresh with stricter parsing alignment and PDE initialization/disturbance fixes.

## [`v1.4.10`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.10) (2024-12-20)

- Reworked statistics and milestone/listener flow across JRE/GWT, plus substantial test updates.

## [`v1.4.9`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.9) (2024-11-18)

- Revisited 3D animation and graph allocation/initialization for more robust rendering and startup behaviour.

## [`v1.4.8`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.8) (2024-08-12)

- Added distribution-building validation checks.

## [`v1.4.7`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.7) (2024-08-12)

- Standardized CLO naming (`--mutation`, `--init`) and aligned tests with parser/species-update refactors.

## [`v1.4.6`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.6) (2024-08-04)

- Fix for `--run` behaviour and help/GUI sizing edge cases.

## [`v1.4.5`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.5) (2024-08-03)

- Continued JRE modernization (`febd72d25`), improved `simulation()` CLO parsing in JRE (`53b662127`), and revised mutant-related CLO parameters (`55a2bd502`).
- Documented cross-architecture Java numeric differences in `Math` (`bbfb96452`), clarifying Intel vs Apple Silicon test-reference mismatches.

## [`v1.4.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.4) (2024-07-29)

- Fixed mutation-related issues and moved fullscreen handling into GWT/web-specific code.

## [`v1.4.3`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.3) (2024-07-27)

- Corrected neighbour sampling, memory allocation in `check()`, and CG option handling.

## [`v1.4.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.2) (2024-07-21)

- Improved model-switch robustness for incompatible parameters and ensured CLO re-parse when needed.

## [`v1.4.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.1) (2024-06-01)

- Reduced javadoc warnings and fixed aggregate doc generation issues.

## [`v1.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4) (2024-05-21)

- Resolved public/private scenario handling (`a5aa2f801`), polished publishing flow (`0f25d8a88`), and automated git-export publishing steps (`46da8bd3f`).
- Reworked graph/view infrastructure: generic graph class work (`5ce437fb7`), tooltip/controller refactors (`83158676d`), and controller/interface cleanup (`f1937e2b3`).
- Added stationary distributions for discrete modules (`f148ca663`), improved tooltips in multi-species modules (`db5e574d3`), and fixed mean-graph rendering behavior (`18bdc4daa`).

## [`v1.3.12`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.12) (2024-04-04)

- Simplified mutation pipelines, especially for continuous traits.

## [`v1.3.11`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.11) (2024-03-21)

- Improved snapshot triggering and histogram/sourcemap behaviour.

## [`v1.3.10`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.10) (2024-03-20)

- Retired stale IBSD init state fields and fixed statistics/sourcemap regressions.

## [`v1.3.9`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.9) (2024-02-19)

- Added second-neighbour geometry support and associated test/export updates.

## [`v1.3.8`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.8) (2024-02-10)

- Fixed geometry rewiring and improved IBSD parsing and test diagnostics.

## [`v1.3.7`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.7) (2024-01-26)

- Added proportional DE updates and clearer handling of imitation variants.

## [`v1.3.6`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.6) (2024-01-23)

- Fixed focal-player selection and addressed CLO parsing/accounting regressions.

## [`v1.3.5`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.5) (2024-01-15)

- Improved testing paths/scripts and multi-species help behaviour.

## [`v1.3.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.4) (2023-12-28)

- Fixed drag-and-drop parsing and plist double-encoding edge cases (`4ea192ea9`).
- Corrected plist double encoding for negative values by switching to long-bit based handling (`6d2b787c9`).

## [`v1.3.3`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.3) (2023-12-20)

- Improved argument splitting/parsing and mutation-range handling.

## [`v1.3.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.2) (2023-12-09)

- Script/link/test-generation cleanup and score-reset API simplification.

## [`v1.3.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.1) (2023-11-22)

- Introduced breaking CLO option updates with static-module and launcher fixes.

## [`v1.3.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.3.0) (2023-11-11)

- Build/version metadata refresh with README and script modernization.

## [`v1.2.5`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2.5) (2023-10-27)

- Added statistics sample-count option and improved JRE simulation flow.

## [`v1.2.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2.4) (2023-10-25)

- Build extension cleanup plus CLO/help/reference-test maintenance.

## [`v1.2.3`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2.3) (2023-10-22)

- Fixed failing tests and made reversed-time ODE/SDE trajectory handling more robust.

## [`v1.2.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2.2) (2023-10-15)

- Improved trait-name parsing and continuous colour-map handling.

## [`v1.2.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2.1) (2023-10-04)

- Patched PDE race conditions and tightened convergence/test behaviour.

## [`v1.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.2) (2023-08-01)

- Fixed RSP color handling (`94e996ba2`) and improved lattice-node shifting in `PopGraph2D` (`f45f2d336`).
- Added point markers (`0b81c6b84`), introduced line-graph zoom/shift controls (`f032646a1`), and fixed pinch-zoom behavior in `PopGraph2D` (`206f8eb71`).

## [`v1.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.1) (2021-04-30)

- Fixed multi-species loading (`fc47d8aef`), followed by broader cleanup (`df057f89e`) and IBS CLO-provider unload hardening (`82bbc9669`).
- Fixed geometry-name encoding (`cfe1e115e`) and improved plist difference reporting (`96219abc6`).

## `pde-tracers` (2020-06-10)

- Retiring PDE tracers.

## [`v1.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.0) (2019-10-16)

- Initial git commit, migrated from `svn`.

## `java3d-expiring` (2019-10-13)

- Last revision with Java3D support.
