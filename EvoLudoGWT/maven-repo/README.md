# EvoLudo Third-Party Local Maven Repository

This directory is a file-based Maven repository for third-party artifacts that
are required by `EvoLudoGWT` but are not available from Maven Central.

It is intentionally committed to git so that 

- no custom maven artifacts need to be installed locally 
- a fresh clone can build with:

```sh
mvn install
```

Current third-party artifacts:

- `org.parallax3d:parallax-gwt:1.6` Note, unfortunately Parallax 3D version 2.0 stayed in beta and never made it into maven central, for more information see https://github.com/thothbot/parallax.
