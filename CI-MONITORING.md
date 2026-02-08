# CI/CD Monitoring & Optimization Guide

## GitHub Actions Minutes Usage

### Current Status
- **Repository Type:** Public
- **Monthly Limit:** ✅ **UNLIMITED** (free for public repos)
- **Cost:** $0/month

> **Note:** GitHub Actions is completely free with unlimited minutes for public repositories. If you ever make this repo private, you'll get 2,000 free minutes/month, then $0.008/minute after that.

---

## Monitoring Your CI Usage

### View Workflow Run Times

```bash
# Check recent workflow durations
gh run list --repo ToastyToast25/VoidStream-FireTV --limit 20 --json name,conclusion,startedAt,updatedAt,databaseId | \
  jq -r '.[] | select(.conclusion == "success") | "\(.name): \((.updatedAt | fromdateiso8601) - (.startedAt | fromdateiso8601)) seconds"'
```

### Current Workflow Build Times (Approximate)

| Workflow | Average Duration | Runs Per Push |
|----------|-----------------|---------------|
| **Store Compliance** | ~4-5 minutes | 1 (matrix: 2 jobs) |
| **Code Quality** | ~2 minutes | 1 |
| **Test** | ~2 minutes | 1 |
| **Security** | ~8-10 minutes | 1 |
| **App / Build** | ~3-4 minutes | 1 |
| **Total per push** | **~15-20 minutes** | **5 workflows** |

### Monthly Usage Estimate

Assuming 100 pushes/month:
- **Total CI minutes:** ~1,500-2,000 minutes/month
- **Cost (if private):** ~$12-16/month (would exceed free tier)
- **Cost (public repo):** **$0** ✅

---

## Optimization Strategies

### ✅ Already Implemented

1. **Path Filters** - Workflows skip on docs-only changes
2. **Concurrency Controls** - Old runs cancelled when new commits pushed
3. **Aggressive Caching** - Gradle cache, dependency cache, build cache
4. **Parallel Jobs** - Store Compliance uses matrix strategy
5. **Build Optimization** - `--parallel --build-cache --configuration-cache`

### 🔧 Additional Optimizations (Optional)

#### 1. Skip CI on Specific Commits

Add `[skip ci]` or `[ci skip]` to commit messages:

```bash
git commit -m "Update README [skip ci]"
```

#### 2. Run Expensive Workflows Only on Master

Change workflow triggers to skip PRs:

```yaml
on:
  push:
    branches:
      - master
  # Remove pull_request trigger for expensive workflows
```

#### 3. Use Self-Hosted Runners (Advanced)

For very high-volume projects, self-hosted runners can be faster and free:
- No minute limits
- Keep dependencies cached locally
- Requires maintenance and server costs

**Verdict:** Not worth it for this project.

#### 4. Reduce Security Scan Frequency

The Security workflow is the longest (~8-10 minutes). You could:

```yaml
on:
  push:
    branches:
      - master
  # Only run on PRs that touch code, not all PRs
  pull_request:
    paths:
      - 'app/**'
      - 'playback/**'
      - '**.gradle*'
  # Keep weekly schedule for comprehensive scans
  schedule:
    - cron: '0 0 * * 0'
```

---

## Monitoring Dashboard

### GitHub UI
1. Go to: https://github.com/ToastyToast25/VoidStream-FireTV/actions
2. Click **"Usage"** in left sidebar (if repo becomes private)
3. View minutes used per workflow

### CLI Monitoring

```bash
# List all workflow runs from last 7 days
gh run list --repo ToastyToast25/VoidStream-FireTV --created ">$(date -d '7 days ago' +%Y-%m-%d)" --limit 100

# Count runs by workflow
gh run list --repo ToastyToast25/VoidStream-FireTV --limit 200 --json name | jq -r '.[].name' | sort | uniq -c | sort -rn

# Check for failed workflows
gh run list --repo ToastyToast25/VoidStream-FireTV --status failure --limit 10
```

---

## Cost Breakdown (If Repo Becomes Private)

### Free Tier Limits (Private Repos)
- **Linux runners:** 2,000 minutes/month free
- **Windows runners:** 1,000 minutes/month free (2x multiplier)
- **macOS runners:** 200 minutes/month free (10x multiplier)

### Current Usage Projection
- **Per push:** ~20 minutes (all Linux runners)
- **100 pushes/month:** ~2,000 minutes
- **200 pushes/month:** ~4,000 minutes ($16/month overage)

### Strategies to Stay Under Free Tier
1. ✅ Use path filters (already implemented)
2. ✅ Add `[skip ci]` for docs-only commits
3. ✅ Run heavy scans (Security) only on master + weekly schedule
4. ✅ Use concurrency controls to cancel old runs
5. Consider limiting PR checks to faster workflows only

---

## Recommended Setup

### Current Setup: ✅ **OPTIMAL for Public Repos**

Your current configuration is excellent for a public repository:
- Comprehensive quality gates
- Fast feedback on PRs
- Security scanning
- Zero cost
- Good caching strategy

### If Repo Becomes Private

Priority order for workflows:
1. **Must Run on Every PR:**
   - Code Quality (fast, catches bugs early)
   - Test (fast, prevents regressions)
   - Store Compliance (fast, ensures policy adherence)

2. **Master Branch Only:**
   - Security (slow, comprehensive)
   - App / Build (medium, artifact generation)

3. **Scheduled Only:**
   - Security / SBOM generation (weekly)
   - Dependabot PRs (weekly)

---

## Action Items

### Now (While Public)
- [x] Nothing! Current setup is optimal.
- [ ] Monitor workflow run times monthly
- [ ] Review failed workflows weekly

### If Repo Goes Private
- [ ] Review this guide and implement cost optimizations
- [ ] Set up GitHub billing alerts at 1,500 minutes
- [ ] Consider limiting Security workflow to master + schedule only

---

## Quick Reference

| Question | Answer |
|----------|--------|
| **Are we wasting CI minutes?** | No - it's free and unlimited for public repos |
| **Will we hit limits?** | No - no limits for public repos |
| **What if we go private?** | Would use ~2,000 min/month (free tier limit) |
| **Should we optimize more?** | Optional - current setup is good |
| **Are builds too slow?** | No - 4-5 min for full compliance checks is fast |

---

## Monitoring Commands

```bash
# Check today's workflow runs
gh run list --repo ToastyToast25/VoidStream-FireTV --created ">$(date +%Y-%m-%d)" --json name,conclusion,startedAt,updatedAt

# View specific workflow history
gh run list --repo ToastyToast25/VoidStream-FireTV --workflow=store-compliance.yaml --limit 20

# Get workflow run details
gh run view --repo ToastyToast25/VoidStream-FireTV <run-id>

# Watch a running workflow
gh run watch --repo ToastyToast25/VoidStream-FireTV <run-id>
```

