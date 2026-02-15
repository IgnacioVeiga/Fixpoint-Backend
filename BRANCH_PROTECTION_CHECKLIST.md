# Branch Protection Checklist (`main`)

Use this checklist in GitHub repository settings before merging `develop` into `main`.

## 1. Require Pull Requests

- Enable branch protection for `main`.
- Require a pull request before merging.
- Require at least 1 approval.
- Dismiss stale approvals when new commits are pushed.

## 2. Require Status Checks

- Require status checks to pass before merging.
- Select backend CI workflow check from `.github/workflows/ci.yml`.
- Require branches to be up to date before merging.

## 3. Restrict Direct Pushes

- Do not allow direct pushes to `main`.
- Restrict who can push to admins/release maintainers only (optional but recommended).

## 4. Conversation and History

- Require conversation resolution before merging.
- Block force pushes.
- Block branch deletion.

## 5. Merge Strategy

- Prefer `Squash and merge` for cleaner history, or:
  - `Rebase and merge` if linear history is required.
- Disable merge methods you do not want to use.

## 6. Post-Merge Verification

- Confirm CI ran on `main` after merge.
- Tag release if needed (`vX.Y.Z`).
- Document noteworthy changes in release notes/changelog.
