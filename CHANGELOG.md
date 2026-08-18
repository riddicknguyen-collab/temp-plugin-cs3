# Changelog

## v2

Fixes found by running v1 against the live site.

- Move the default domain to `yanhh3d.pw`; `yanhh3d.love` and `yanhh3d.ac` stay as
  remap sources so existing bookmarks and watch history keep resolving.
- Read card posters from `data-src`. The site lazy-loads images and the `img`
  element carries no `src` at all, so every poster came back empty.
- Declare `TvType.Anime`. CloudStream groups providers by `supportedTypes`, and
  without it YanHH3D never appeared under the animation media filter.
- Send `Referer` and `User-Agent` with poster requests, since CloudStream loads
  images through its own loader.
- Fall back to the card heading when the anchor has no `title`.
- Home fixture rebuilt from the live page markup.

## v1

- Initial YanHH3D provider.
- Home, category, search, detail and episode support.
- Direct HLS sources with `Referer` and `User-Agent`; embed hosts delegated to CloudStream extractors.
- Domain resolver so a YanHH3D domain change is a one-constant edit, and stored episode
  URLs stay path-only so watch history survives that change.
- Parser covered by HTML fixtures under `YanHHProvider/src/test/resources/yanhh3d`.
