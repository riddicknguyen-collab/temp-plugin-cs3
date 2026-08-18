# Changelog

## v1

- Initial YanHH3D provider.
- Home, category, search, detail and episode support.
- Direct HLS sources with `Referer` and `User-Agent`; embed hosts delegated to CloudStream extractors.
- Domain resolver so a YanHH3D domain change is a one-constant edit, and stored episode
  URLs stay path-only so watch history survives that change.
- Parser covered by HTML fixtures under `YanHHProvider/src/test/resources/yanhh3d`.
