#!/bin/bash

# Script to manually update CHANGELOG.md with commits since last tag
# Usage: ./scripts/update-changelog.sh [version]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the version
if [ -z "$1" ]; then
    echo -e "${YELLOW}No version specified, using latest tag${NC}"
    VERSION=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    if [ -z "$VERSION" ]; then
        echo -e "${RED}Error: No tags found and no version specified${NC}"
        exit 1
    fi
else
    VERSION="$1"
fi

# Validate version format
if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
    echo -e "${RED}Error: Version must follow semantic versioning (e.g., v1.2.0)${NC}"
    exit 1
fi

echo -e "${GREEN}Updating CHANGELOG.md for version ${VERSION}${NC}"

# Get the current date
DATE=$(date +%Y-%m-%d)

# Get previous tag
PREVIOUS_TAG=$(git describe --tags --abbrev=0 "${VERSION}^" 2>/dev/null || echo "")

if [ -z "$PREVIOUS_TAG" ]; then
    echo -e "${YELLOW}No previous tag found, using all commits${NC}"
    COMMITS=$(git log --pretty=format:"- %s" --no-merges)
else
    echo -e "${GREEN}Comparing ${PREVIOUS_TAG}..${VERSION}${NC}"
    COMMITS=$(git log --pretty=format:"- %s" --no-merges "${PREVIOUS_TAG}..${VERSION}")
fi

# Check if there are any commits
if [ -z "$COMMITS" ]; then
    echo -e "${YELLOW}No commits found for this version${NC}"
    exit 0
fi

# Create new version entry
NEW_ENTRY="## [${VERSION}] - ${DATE}\n\n"

# Categorize commits
FEATURES=$(echo "$COMMITS" | grep -E "^-\s*(feat|add)" || true)
FIXES=$(echo "$COMMITS" | grep -E "^-\s*fix" || true)
TESTS=$(echo "$COMMITS" | grep -E "^-\s*test" || true)
REFACTORS=$(echo "$COMMITS" | grep -E "^-\s*refactor" || true)
CHORES=$(echo "$COMMITS" | grep -E "^-\s*chore" || true)
DOCS=$(echo "$COMMITS" | grep -E "^-\s*docs" || true)

# Build sections
ADDED=false
if [ -n "$FEATURES" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Added\n${FEATURES}\n\n"
    ADDED=true
fi

if [ -n "$FIXES" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Fixed\n${FIXES}\n\n"
    ADDED=true
fi

if [ -n "$TESTS" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Tests\n${TESTS}\n\n"
    ADDED=true
fi

if [ -n "$REFACTORS" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Changed\n${REFACTORS}\n\n"
    ADDED=true
fi

if [ -n "$CHORES" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Chores\n${CHORES}\n\n"
    ADDED=true
fi

if [ -n "$DOCS" ]; then
    NEW_ENTRY="${NEW_ENTRY}### Documentation\n${DOCS}\n\n"
    ADDED=true
fi

if [ "$ADDED" = false ]; then
    echo -e "${YELLOW}No categorized commits found${NC}"
    NEW_ENTRY="${NEW_ENTRY}### Changed\n${COMMITS}\n\n"
fi

# Check if CHANGELOG.md exists
if [ ! -f "CHANGELOG.md" ]; then
    echo -e "${YELLOW}CHANGELOG.md not found, creating new one${NC}"
    cat > CHANGELOG.md << 'EOF'
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

EOF
fi

# Create temporary file with new entry
{
    echo "# Changelog"
    echo ""
    echo "All notable changes to this project will be documented in this file."
    echo ""
    echo "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),"
    echo "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."
    echo ""
    echo "## [Unreleased]"
    echo ""
    echo -e "$NEW_ENTRY"
    # Get existing changelog content after Unreleased section
    tail -n +7 CHANGELOG.md 2>/dev/null | grep -v "^## \[Unreleased\]" || true
} > CHANGELOG.md.new

mv CHANGELOG.md.new CHANGELOG.md

# Update comparison links
if [ -n "$PREVIOUS_TAG" ]; then
    # Remove trailing empty lines
    while [ "$(tail -c 1 CHANGELOG.md | wc -l)" -gt 0 ]; do
        truncate -s -1 CHANGELOG.md
    done
    
    # Check if link already exists
    if ! grep -q "^\[${VERSION}\]:" CHANGELOG.md; then
        echo "" >> CHANGELOG.md
        echo "[${VERSION}]: https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]\(.*\)\.git/\1/')/compare/${PREVIOUS_TAG}...${VERSION}" >> CHANGELOG.md
    fi
fi

echo -e "${GREEN}CHANGELOG.md updated successfully!${NC}"
echo -e "${YELLOW}Review the changes and commit them:${NC}"
echo "  git add CHANGELOG.md"
echo "  git commit -m \"docs: update CHANGELOG for ${VERSION}\""
