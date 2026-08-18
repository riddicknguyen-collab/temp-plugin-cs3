# Changelog

## v3

Rebuilt the detail and episode parsing against real pages saved from the site. The
PRD's page model was wrong, which is why every title showed "coming soon".

- Episodes come from the watch page, not the detail page. The detail page carries no
  episode list at all, only one play button per server, so `load()` now follows that
  button and parses the list from the page it leads to.
- Only the Vietsub server is used. Each title is published twice, "Thuyết Minh" and
  "Vietsub"; the play button and the episode tab are both picked by that label.
- Metadata is read from the `.anisc-info` rows (`span.item-head` plus `span.name`),
  scoped to that block. The sidebar menu repeats the same label words and links every
  genre on the site, so an unscoped search returned the site menu as the title's genres.
- Episodes are labelled with the bare number on the page, so a numeric label is
  displayed as "Tập N".
- Detail and episode fixtures rebuilt from the saved pages.

Known gap: an episode's "HD" entry is a Facebook CDN play URL rather than a playlist or
a supported embed host, so it is classified unknown and skipped. The 1080 and 4K
playlists on the same page are unaffected.

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
