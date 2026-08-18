# Changelog

## v8

Fix playback for the Vietsub `sever2` sources that reached ExoPlayer but came back as
HTTP status failures (`ERROR_CODE_IO_BAD_HTTP_STATUS`, app error 2004).

- Resolve each `data-src` player page with the episode page as the `Referer` instead of
  the site root, so the issued stream token stays tied to the same watch-page context
  the browser uses.
- Send full HLS request headers on emitted media links: `User-Agent`, `Referer`
  (episode page), and `Origin` (site root). The previous build only kept
  `User-Agent` in `headers`, which could let the manifest or segments fail on stricter
  CDN checks.
- Pass the episode page as extractor referer too, so non-HLS fallback hosts see the
  same context.

## v7

Offer every server the site does, including the one that is not HLS.

- The "HD" server was dropped because its URL has no `.m3u8` in it. Fetching it shows a
  jwplayer page whose source is a single progressive MP4, so it is now offered as a
  direct video link. Classification no longer guesses from the URL shape: every
  non-embed source is fetched and the body decides whether it is a playlist, a player
  config, or a progressive file.
- When a server's label carries no quality ("HD"), the resolved URL is read for one
  instead, which recovers 720p for that server.

## v6

v5 assumed every source is a player page, so anything else was dropped and the app
reported no links at all.

- Check what a source actually returns instead of assuming. A body starting with
  `#EXTM3U` is already a playlist and is used as-is; otherwise the player config is
  read out of it as before.
- Accept the config blob from any element carrying `data-obf`, not only `#player`.
- Log the response size when a source page is not recognised, so an unreachable host
  can be told apart from a page whose shape changed.

## v5

Playback fixed properly, and v4's guess reverted.

A server's `data-src` ends in `.m3u8` but serves an **HTML player page**, which is why
the player reported error 3002: it was handed markup, not a manifest. The page carries
its config as base64 JSON in `<div id="player" data-obf="...">`, and the playable
manifest is the `pU` entry. `loadLinks()` now fetches that page per source and reads
the manifest out of it. The token in the URL is issued per request, so this has to
happen at playback time and cannot be cached.

The config also offers an AES-GCM encrypted variant and its key; those are ignored in
favour of the plain playlist the site already serves.

Reverts v4's `/stream/m3u8/<file>` rewrite, which returned HTTP 500. It came from the
reference JS implementation and was never verified against the CDN.

Sources whose player page cannot be read are now dropped rather than emitted, so a
broken entry never reaches the player.

## v4

- Rewrite playlist URLs onto `/stream/m3u8/<file>`, which is where the CDN actually
  serves them. The path advertised in `data-src` returns something that is not a
  manifest, so playback failed with player error 3002
  (`ERROR_CODE_PARSING_MANIFEST_MALFORMED`). Embed and unknown sources are untouched.

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
