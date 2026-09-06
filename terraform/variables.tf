variable "github_owner" {
  type        = string
  description = "GitHub user or org that owns the repository."
  default     = "bhargavms"
}

variable "github_repository" {
  type        = string
  description = "Repository name (not owner/name)."
  default     = "ksp-mappergen"
}

variable "master_ruleset_id" {
  type        = number
  description = "Existing repository ruleset id for the default branch. Used by the import block."
  default     = 11297839
}
