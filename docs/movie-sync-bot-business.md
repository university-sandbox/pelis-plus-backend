# Movie Sync Bot

## Purpose

The Movie Sync Bot keeps the movie catalog updated automatically so the business team does not need to manually create or refresh movie records every week.

Its main goal is to make sure the platform always has current movie information available when creating screenings, including titles, summaries, posters, release dates, duration, popularity data, genres, and trailer references when available.

## What The Bot Does

The bot checks the external movie source once per week and refreshes the local movie catalog based on what is currently available.

By default, it runs every Sunday at 11:00 PM and reviews the current week of movie availability. This gives the system an updated catalog before the next business week starts.

The bot only manages movie records. It does not change screenings, rooms, orders, tickets, users, payments, memberships, snacks, or any other business data.

## Business Rules

- New available movies are added automatically.
- Movies already in the system are not downloaded again unless they are missing.
- Movies currently available remain active.
- Movies no longer considered available are marked inactive.
- Inactive movies stay in the system for history and reference.
- Only the movie catalog is affected.

## Why This Matters

This automation reduces manual work for administrators and lowers the risk of missing or outdated movie information.

When the team creates screenings, the movie list should already be updated with the latest available movies. This helps avoid manual data entry, missing durations, incomplete descriptions, or outdated posters.

## Schedule

The bot runs automatically once per week:

- Day: Sunday
- Time: 11:00 PM
- Time zone: America/Lima
- Default movie window: current week
- Maximum configurable window: up to three weeks

The weekly schedule is designed to pull the latest available movies and update the catalog for the upcoming operational week.

## Manual Run

The bot can also be run manually by the backend team when needed.

Typical reasons to run it manually:

- A new movie needs to appear before the next scheduled run.
- The external source was temporarily unavailable during the automatic run.
- The team wants to refresh the catalog after changing availability settings.
- A deployment happened before the regular weekly update.

## Expected Outcome

After the bot runs:

- New movies for the current period are available in the catalog.
- Existing available movies remain active.
- Movies no longer available are inactive.
- Movie information is ready for screening creation.
- Other business records remain unchanged.

## Important Limits

The bot depends on the external movie source being available and returning accurate information.

If a movie is missing from the external source, the bot cannot add it automatically. In that case, the backend or admin team may need to review it manually.

The bot does not decide business programming strategy. It only keeps the movie catalog aligned with the available movie data source.

## Ownership

Business owner: Operations or content management team.

Technical owner: Backend team.

The business team should define the availability window and operational expectations. The backend team should maintain the automation and verify that it continues to run successfully.
