# GitHub ruleset (local Terraform)

Do not run this in CI. Use your machine and a token that can admin the repo.

`master` is not using classic branch protection. It already has ruleset **master** (`11297839`): squash-only, 1 review, required check `ci`, CodeQL, no force-push, no direct push. This config imports that ruleset instead of creating a second one. It also creates the `maven-central` Actions environment used by the Publish workflow, limited to `master` and `v*` tags.

```bash
cd terraform
export GITHUB_TOKEN="$(gh auth token)"
terraform init
terraform plan
```

If the plan only shows an import and no GitHub updates, apply:

```bash
terraform apply
```

If the plan wants to drop `code_quality` or `require_extra_approval_for_unattributed_changes`, that is a provider gap. Do not apply until you are fine losing those two live settings.
