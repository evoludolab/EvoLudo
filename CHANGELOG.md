# Changelog

All notable project revisions are listed here in reverse chronological order.

## [`v2.2.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.2.0) (2026-02-18) Public release: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14591549.svg)](https://zenodo.org/doi/10.5281/zenodo.14591549)

- Upgraded GWT to `2.13` ([`bd73624fa`](https://github.com/evoludolab/EvoLudo/commit/bd73624fa8342f3cbf5326de9c993c8c56a0439e)), added Maven repo packaging ([`922d7f5e9`](https://github.com/evoludolab/EvoLudo/commit/922d7f5e90f7fdff5b74e112a8efec9cee09f74c)), and improved publish scripting ([`cb6f86bd1`](https://github.com/evoludolab/EvoLudo/commit/cb6f86bd1425dcdcec3fd8230366aff52c119d46)).
- Fixed the delay default ([`33fd9b6bf`](https://github.com/evoludolab/EvoLudo/commit/33fd9b6bfbcb610877c0cd1467ad7bf4aea66597)), refreshed README docs ([`f75725aea`](https://github.com/evoludolab/EvoLudo/commit/f75725aea9fb6c92ecfc34ed163bf2bbc1dc2a72)), and added missing copyright headers ([`4cef9bdf8`](https://github.com/evoludolab/EvoLudo/commit/4cef9bdf824e4e906162adb0e80ae8fd01c92622)).
- Refined hierarchy/geometry integration ([`2f405ce22`](https://github.com/evoludolab/EvoLudo/commit/2f405ce22ac5e9e361fa5584135bfc29d25d2999)), consolidated lattice-dimension checks ([`a2ab958fc`](https://github.com/evoludolab/EvoLudo/commit/a2ab958fceea296b3db932483a57ba258ebc7d4f)), and fixed second-neighbour boundary handling ([`feff5aa95`](https://github.com/evoludolab/EvoLudo/commit/feff5aa954c3d32de268a5868d69b01e0c05adc8)).
- Streamlined GWT view-update throttling ([`882d422a8`](https://github.com/evoludolab/EvoLudo/commit/882d422a81b0d3252341422d323f5ddcb97846d7)), improved histogram vertical sizing ([`23f6722ee`](https://github.com/evoludolab/EvoLudo/commit/23f6722eebd5a8f27ea0b18223a3d144e4d98fdd)), and fixed S3 initial-state handling ([`0d33005ba`](https://github.com/evoludolab/EvoLudo/commit/0d33005bae7b4a3e7fa03329b3548f9d7db6c1fa)).
- Improved graph GUI controls: added `ParaGraph` zoom/shift interactions ([`b5c6d19c3`](https://github.com/evoludolab/EvoLudo/commit/b5c6d19c364f6b425debc356bf3999f9ca47f425)) and enabled y-axis label placement to switch between left and right ([`2457cda37`](https://github.com/evoludolab/EvoLudo/commit/2457cda379417a70526e53cc3b115b400f872e39)).
- Streamlined the context menu, including dedicated zoom and axis submenus ([`7d972a36a`](https://github.com/evoludolab/EvoLudo/commit/7d972a36ae9fb81e1d1a76be7b5d7d32494e8f27), [`522f7e77e`](https://github.com/evoludolab/EvoLudo/commit/522f7e77ebb923c62a5962edcbf2a10b36e1f1a7)).
- Refactored `EvoLudoWeb` view control ([`cbe84221a`](https://github.com/evoludolab/EvoLudo/commit/cbe84221a176a45b2e187a5ff5719e47fa3cf75d)), extracted `CLOController` ([`ffecb236b`](https://github.com/evoludolab/EvoLudo/commit/ffecb236bf7687300578f5db3e860bc9b273080a)), and extracted `RunController` ([`323577b11`](https://github.com/evoludolab/EvoLudo/commit/323577b1141499d51fc29a52b72fd5582bb2b2a8)).
- Introduced major geometry architecture updates: new geometry package/class rollout ([`a38ec4120`](https://github.com/evoludolab/EvoLudo/commit/a38ec4120e87263b557b426824f605c36f041bbf)), hierarchical-geometry rework ([`1935d000d`](https://github.com/evoludolab/EvoLudo/commit/1935d000d81e69b5453af05db76ef67611c4ca6e)), and geom-package cleanup/renaming ([`9888198b5`](https://github.com/evoludolab/EvoLudo/commit/9888198b5b628a04a4738d03370d10f3d0712670)).
- Expanded multi-species ecological updates in `LV` ([`3ab27e2ee`](https://github.com/evoludolab/EvoLudo/commit/3ab27e2eeb02abf68bdd232900ea54a76ad8a05d)), added DE cross-species competition ([`bf1dfc476`](https://github.com/evoludolab/EvoLudo/commit/bf1dfc47634b88ad3dd60438665344b4a3115f51)), and improved `ParaGraph` autoscaling/clamping for `LV` ([`3408de599`](https://github.com/evoludolab/EvoLudo/commit/3408de5996f23a3b4d7d911b75132f40ab367fca)).
- Hardened module/model load-unload and CLO handling: model load flow updates ([`e284ea4b6`](https://github.com/evoludolab/EvoLudo/commit/e284ea4b6b2341f2b10ab9e1962ca244b30af626)), provider-aware model (un)load ([`412d982cf`](https://github.com/evoludolab/EvoLudo/commit/412d982cf73bcedd1895ef66b334713cd01cea71)), and stricter CLO parser behavior ([`1de37827f`](https://github.com/evoludolab/EvoLudo/commit/1de37827f55d355217e745ac7225273328258c29)).

## [`v2.1.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.1.1) (2025-07-28)

- Added a null-safety guard for `activeModel` ([`3fb712564`](https://github.com/evoludolab/EvoLudo/commit/3fb712564ee995a74b9c8b4c1e430baed4f8713f)) and updated README documentation links ([`065c6bcfe`](https://github.com/evoludolab/EvoLudo/commit/065c6bcfe9471ea0ca7620e44b5fdf855fbd6be1)).

## [`v2.1.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.1.0) (2025-07-28)

- Added the `LV` module ([`cea81d7c4`](https://github.com/evoludolab/EvoLudo/commit/cea81d7c48d168db05ea09065d17e996af7c51f3)), fixed SDE behavior for multi-species/frequency dynamics ([`9df4761d2`](https://github.com/evoludolab/EvoLudo/commit/9df4761d21a5ab12ad60da874d1d9dc3f24f0b7b)), and added dual DE dynamics support ([`3a0b86c01`](https://github.com/evoludolab/EvoLudo/commit/3a0b86c014cfb3f523bd4f1633af5ee0eb254d2f)).
- Improved realtime increment handling ([`c4dd004bc`](https://github.com/evoludolab/EvoLudo/commit/c4dd004bc7c427034ab222e7d98ff7bcc58839a8)), refreshed tests for realtime changes ([`e624941e7`](https://github.com/evoludolab/EvoLudo/commit/e624941e7c7ea65586833a52752a3fa44342b0ed)), and fixed SDE statistics reporting ([`d7699fb80`](https://github.com/evoludolab/EvoLudo/commit/d7699fb80f9f9b959a781e6386c20e5333083445)).
- Refactored model interfaces around DE/payoff semantics: `Scores` to `Payoffs` rename ([`d74f7805f`](https://github.com/evoludolab/EvoLudo/commit/d74f7805f83180a7c9bc1ae5d1d645abb0995104)), `HasDE` API tightening ([`612da3497`](https://github.com/evoludolab/EvoLudo/commit/612da3497ac6fd2ab147ecb1e6f54d135b47a6b0)), and DE-dependent/multispecies abstraction changes ([`9b3591ab8`](https://github.com/evoludolab/EvoLudo/commit/9b3591ab83dfbed5d480ca8b038101ab9825273a)).
- Continued SIR/ecological model improvements: SIR module introduction ([`2270e4e71`](https://github.com/evoludolab/EvoLudo/commit/2270e4e71ed1cecebf96fd8c412c15643331017a)), improved optional transition defaults ([`ff0c14868`](https://github.com/evoludolab/EvoLudo/commit/ff0c14868bdadffee88ae2127820f89a55c92377)), and follow-up SIR cleanup ([`b8f49d343`](https://github.com/evoludolab/EvoLudo/commit/b8f49d3438f1bf4b5df158f822a246f0ffcf5afd)).

## [`v2.0.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.2) (2025-03-09)

- Consolidated javadoc work ([`773bd0e28`](https://github.com/evoludolab/EvoLudo/commit/773bd0e28f13bcda08294e01b1f12dcae98b3b7c)), added DOI/badge updates in README ([`bf58ecec8`](https://github.com/evoludolab/EvoLudo/commit/bf58ecec8cb2e294834299e8d869d5bf5f4c99ba)), and improved test generation scripting ([`850099971`](https://github.com/evoludolab/EvoLudo/commit/8500999719512f3ddd752069f33fe12c21af01bb)).

## [`v2.0.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.1) (2025-01-31)

- Fixed javadoc issues ([`b03cf4874`](https://github.com/evoludolab/EvoLudo/commit/b03cf4874c2229ba98def7e6f70af1229f9075b0)), ensured IBS memory allocation in `check()` ([`5f6e8fa93`](https://github.com/evoludolab/EvoLudo/commit/5f6e8fa938def7ab1ec89dc0304cc9ab9f531cf4)), adjusted listener ordering ([`0b4df073d`](https://github.com/evoludolab/EvoLudo/commit/0b4df073d26a8ebef266812dd9dd7fe12d5fe738)), and improved statistics checks ([`9ac55a3b9`](https://github.com/evoludolab/EvoLudo/commit/9ac55a3b9122625ab7c87e090e2e55c366c3bd74)).

## [`v2.0.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v2.0.0) (2025-01-03) Initial public release: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14591549.svg)](https://zenodo.org/doi/10.5281/zenodo.14591549)

- Removed unnecessary population load/unload churn ([`c45b71449`](https://github.com/evoludolab/EvoLudo/commit/c45b714497d16983443655d61f1ee07465afb200)), fixed CLO-related testing issues ([`59d3a0de3`](https://github.com/evoludolab/EvoLudo/commit/59d3a0de30d2652601054d228e09d6811c8b7d03)), added GWT `--help` support ([`59ee1de90`](https://github.com/evoludolab/EvoLudo/commit/59ee1de907df73882cdfbdb269fb9dc0c46cd959)), and updated continuous-mutation naming ([`e9dfaedb4`](https://github.com/evoludolab/EvoLudo/commit/e9dfaedb4dc64b88cb258723d5929cbef8322061)).

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

- Continued JRE modernization ([`febd72d25`](https://github.com/evoludolab/EvoLudo/commit/febd72d252cfaa689b7c179364b4a8a04f243853)), improved `simulation()` CLO parsing in JRE ([`53b662127`](https://github.com/evoludolab/EvoLudo/commit/53b662127b86304246a54aa471e58995ec119fb2)), and revised mutant-related CLO parameters ([`55a2bd502`](https://github.com/evoludolab/EvoLudo/commit/55a2bd50282a36ac432e0c39d3634570fbaa5146)).
- Documented cross-architecture Java numeric differences in `Math` ([`bbfb96452`](https://github.com/evoludolab/EvoLudo/commit/bbfb964524ece8ea43e4adb22e749547d6c7e305)), clarifying Intel vs Apple Silicon test-reference mismatches.

## [`v1.4.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.4) (2024-07-29)

- Fixed mutation-related issues and moved fullscreen handling into GWT/web-specific code.

## [`v1.4.3`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.3) (2024-07-27)

- Corrected neighbour sampling, memory allocation in `check()`, and CG option handling.

## [`v1.4.2`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.2) (2024-07-21)

- Improved model-switch robustness for incompatible parameters and ensured CLO re-parse when needed.

## [`v1.4.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4.1) (2024-06-01)

- Reduced javadoc warnings and fixed aggregate doc generation issues.

## [`v1.4`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.4) (2024-05-21)

- Resolved public/private scenario handling ([`a5aa2f801`](https://github.com/evoludolab/EvoLudo/commit/a5aa2f801d5b0f4aaae7ca618219cfa6f1939184)), polished publishing flow ([`0f25d8a88`](https://github.com/evoludolab/EvoLudo/commit/0f25d8a880dcf1b62dae0780f135614d9b0c5909)), and automated git-export publishing steps ([`46da8bd3f`](https://github.com/evoludolab/EvoLudo/commit/46da8bd3f5bfeab87218453b065429d0c9907563)).
- Reworked graph/view infrastructure: generic graph class work ([`5ce437fb7`](https://github.com/evoludolab/EvoLudo/commit/5ce437fb71d4a8d7bd0882cc27cec9aa2f5f80ec)), tooltip/controller refactors ([`83158676d`](https://github.com/evoludolab/EvoLudo/commit/83158676d7356bf9a11312a64908ee5344332f2d)), and controller/interface cleanup ([`f1937e2b3`](https://github.com/evoludolab/EvoLudo/commit/f1937e2b37f650f294f424e8587e8875958b61c2)).
- Added stationary distributions for discrete modules ([`f148ca663`](https://github.com/evoludolab/EvoLudo/commit/f148ca663b076455845eec8d7cb9450516054890)), improved tooltips in multi-species modules ([`db5e574d3`](https://github.com/evoludolab/EvoLudo/commit/db5e574d3218cfb4e88095b17f2e623751145ea1)), and fixed mean-graph rendering behavior ([`18bdc4daa`](https://github.com/evoludolab/EvoLudo/commit/18bdc4daa28449929f6c9d136bc15beb54959031)).

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

- Fixed drag-and-drop parsing and plist double-encoding edge cases ([`4ea192ea9`](https://github.com/evoludolab/EvoLudo/commit/4ea192ea99c4100568c321dbc89bec82d65731d9)).
- Corrected plist double encoding for negative values by switching to long-bit based handling ([`6d2b787c9`](https://github.com/evoludolab/EvoLudo/commit/6d2b787c95fbfb0866be143032ab8b1b640ae678)).

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

- Fixed RSP color handling ([`94e996ba2`](https://github.com/evoludolab/EvoLudo/commit/94e996ba2c30454b54b203bbd31ff9c1cab174d6)) and improved lattice-node shifting in `PopGraph2D` ([`f45f2d336`](https://github.com/evoludolab/EvoLudo/commit/f45f2d336f379f8b985d236b9eb9c4dae993928b)).
- Added point markers ([`0b81c6b84`](https://github.com/evoludolab/EvoLudo/commit/0b81c6b8414c3603f9c6adaa0907985d1cbe5af2)), introduced line-graph zoom/shift controls ([`f032646a1`](https://github.com/evoludolab/EvoLudo/commit/f032646a161bae14704e4991ab6f167584a1c80d)), and fixed pinch-zoom behavior in `PopGraph2D` ([`206f8eb71`](https://github.com/evoludolab/EvoLudo/commit/206f8eb71eed075c18e78416e4b4cf514bafde50)).

## [`v1.1`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.1) (2021-04-30)

- Fixed multi-species loading ([`fc47d8aef`](https://github.com/evoludolab/EvoLudo/commit/fc47d8aef6310ae190e622953a290ae6082b7084)), followed by broader cleanup ([`df057f89e`](https://github.com/evoludolab/EvoLudo/commit/df057f89e062c5c6ec6c9e5745799877896e6316)) and IBS CLO-provider unload hardening ([`82bbc9669`](https://github.com/evoludolab/EvoLudo/commit/82bbc966931003dd3b4519e414cfbe3b30bde508)).
- Fixed geometry-name encoding ([`cfe1e115e`](https://github.com/evoludolab/EvoLudo/commit/cfe1e115e6257290c42c44799242333538d525e8)) and improved plist difference reporting ([`96219abc6`](https://github.com/evoludolab/EvoLudo/commit/96219abc66fec390dcb83744afa3c07637a00391)).

## `pde-tracers` (2020-06-10)

- Retiring PDE tracers.

## [`v1.0`](https://github.com/evoludolab/EvoLudo/releases/tag/v1.0) (2019-10-16)

- Initial git commit, migrated from `svn`.

## `java3d-expiring` (2019-10-13)

- Last revision with Java3D support.
