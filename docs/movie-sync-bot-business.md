# Movie Sync Bot

## Purpose

The Movie Sync Bot keeps the MVP movie experience updated automatically so the business team does not need to manually create or refresh movies and basic functions every week.

Its main goal is to make sure the platform has enough data to populate these customer-facing sections:

- Estrenos
- Cartelera
- Proximos estrenos
- Populares esta semana

The bot is not a final programming or pricing engine. For the MVP it simulates the operational work that would normally be done by cinema staff: importing movie data, keeping catalog sections fresh, creating basic functions for movies in cartelera, and removing stale functions when they are safe to remove.

## What The Bot Does

The bot checks TMDB once per week and refreshes local movie availability from three TMDB lists:

- `now_playing`: movies released recently and currently relevant to show in cinema.
- `upcoming`: future releases used for Proximos estrenos.
- `popular`: popular movies used for Populares esta semana.

For movies in `now_playing`, the bot also creates basic active functions when the movie has no active future function. This makes Cartelera work without manual admin setup during the MVP.

By default, it runs every Sunday at 11:00 PM America/Lima and reviews the configured release window.

## Business Rules

- New movies from TMDB are added automatically.
- Existing movies are not downloaded again unless they are missing locally.
- Movies found in the current TMDB sync remain active.
- Movies no longer found in the sync may become inactive, but only when they do not still have active functions.
- Proximos estrenos and Populares esta semana are movie-only sections; the bot does not create functions for them.
- Cartelera is based on active functions. A movie appears in Cartelera only when it has at least one active function.
- For MVP simulation, each synced `now_playing` movie without active future functions receives a small generated schedule.
- Generated functions use simple default dates, times, formats, rooms, and prices. Exact combinations are not business-critical for the MVP.
- Stale future functions are cancelled only when no client has bought a ticket for them.
- Functions with bought tickets remain active to protect the customer purchase and ticket history.
- Inactive movies and cancelled functions stay in the system for history and reference.

## Section Behavior

### Estrenos

Estrenos uses active movies with status `now_playing`. These are recently released movies returned by TMDB and filtered by the configured release lookback window.

### Cartelera

Cartelera is computed from active functions, not from the movie status alone.

The bot creates MVP functions for `now_playing` movies that do not already have active future functions. If a movie has active functions, it can appear in Cartelera.

When a movie is no longer in the current `now_playing` result, the bot cancels its future active functions only if those functions have no bought tickets.

### Proximos Estrenos

Proximos estrenos uses active movies with status `upcoming`.

These movies are catalog-only. The bot does not create functions for them because they are not yet in Cartelera.

### Populares Esta Semana

Populares esta semana uses active movies with status `popular`.

These movies are catalog-only. The bot does not create functions for them unless they also appear in `now_playing`, in which case the `now_playing` behavior has priority.

## Why This Matters

This automation reduces manual work for administrators and lowers the risk of empty or stale movie sections.

For the MVP, it also makes the product feel operational without requiring the team to manually build every function schedule. Customers can see movies in Cartelera because the bot creates basic functions, while future and popular movies remain visible as catalog sections.

## Schedule

The bot runs automatically once per week:

- Day: Sunday
- Time: 11:00 PM
- Time zone: America/Lima
- Default movie window: current week
- Maximum configurable window: controlled by `TMDB_MAX_RELEASE_LOOKBACK_DAYS`

The weekly schedule is designed to pull the latest available movies, refresh catalog sections, and generate enough basic functions for the upcoming operational week.

## Manual Run

The bot can also be run manually by the backend team when needed.

Typical reasons to run it manually:

- A new movie needs to appear before the next scheduled run.
- The external source was temporarily unavailable during the automatic run.
- The team wants to refresh catalog sections after changing availability settings.
- A deployment happened before the regular weekly update.
- The team wants to regenerate missing MVP functions for Cartelera.

## Expected Outcome

After the bot runs:

- Estrenos has recently released active movies.
- Cartelera has movies with active functions.
- Proximos estrenos has upcoming movie records.
- Populares esta semana has popular movie records.
- Missing MVP functions are created for current `now_playing` movies.
- Stale ticketless functions are cancelled.
- Ticketed functions remain active.
- Movie information is ready for browsing and ticket purchase flows.

## Important Limits

The bot depends on TMDB being available and returning accurate information.

If a movie is missing from TMDB, the bot cannot add it automatically. In that case, the backend or admin team may need to review it manually.

The generated functions are intentionally simple. They are meant to simulate cinema programming for the MVP, not optimize rooms, demand, formats, prices, or showtime strategy.

The bot protects bought tickets by keeping ticketed functions active even when the movie is no longer in the current TMDB `now_playing` list.

## Ownership

Business owner: Operations or content management team.

Technical owner: Backend team.

The business team should define the availability window and operational expectations. The backend team should maintain the automation and verify that it continues to run successfully.
