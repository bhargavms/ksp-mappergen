# Classic branch protection on master is absent (API 404).
# Live protection is repository ruleset "master" (id 11297839), fetched 2026-09-06.
#
# Provider cannot express these live fields; the first apply may drop them:
#   - rules.code_quality (severity = errors)
#   - pull_request.require_extra_approval_for_unattributed_changes = true
# Review `terraform plan` before apply.
#
# Auth: export GITHUB_TOKEN="$(gh auth token)"  (needs repo admin)

import {
  to = github_repository_ruleset.master
  id = "${var.github_repository}:${var.master_ruleset_id}"
}

resource "github_repository_ruleset" "master" {
  name        = "master"
  repository  = var.github_repository
  target      = "branch"
  enforcement = "active"

  conditions {
    ref_name {
      include = ["~DEFAULT_BRANCH"]
      exclude = []
    }
  }

  # Repository admin (role id 5) can bypass.
  bypass_actors {
    actor_id    = 5
    actor_type  = "RepositoryRole"
    bypass_mode = "always"
  }

  rules {
    creation                = true
    update                  = true
    deletion                = true
    non_fast_forward        = true
    required_linear_history = true

    pull_request {
      required_approving_review_count   = 1
      dismiss_stale_reviews_on_push     = false
      require_code_owner_review         = false
      require_last_push_approval        = false
      required_review_thread_resolution = false
      allowed_merge_methods             = ["squash"]
    }

    required_status_checks {
      strict_required_status_checks_policy = true
      do_not_enforce_on_create             = false

      required_check {
        context        = "ci"
        integration_id = 15368 # GitHub Actions
      }
    }

    required_code_scanning {
      required_code_scanning_tool {
        tool                      = "CodeQL"
        alerts_threshold          = "errors"
        security_alerts_threshold = "high_or_higher"
      }
    }
  }
}
