# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Hugo-based personal blog with a custom theme. The site is deployed to GitHub Pages via GitHub Actions and serves content from https://andrewrea.co.uk/.

## Key Commands

### Development
```bash
# Run Hugo development server locally
hugo server --disableFastRender --bind "0.0.0.0" --minify --theme book

# Update git submodules to latest
git submodule foreach git pull origin master

# Build the site
hugo --minify
```

### Deployment
- Deployment is automated via GitHub Actions (`.github/workflows/hugo.yml`)
- Pushes to `main` branch trigger automatic deployment to GitHub Pages
- The workflow builds with Hugo 0.114.0 and deploys to the `gh-pages` environment

## Architecture

### Content Structure
- `/content/posts/` - Blog posts in Markdown format
- `/content/pages/` - Static pages (like About)
- `/content/shorts/` - Short-form content
- `/static/` - Static assets (CSS, JS, images)

### Theme Structure
- Uses custom theme `reaandrew` located in `/themes/reaandrew/`
- Theme includes custom layouts for posts, pages, and navigation
- Bootstrap CSS framework is included in static assets

### Configuration
- Main site config in `config.toml`
- Site URL: `https://andrewrea.co.uk/`
- Author: Andy Rea with social media links configured
- Goldmark renderer with unsafe HTML enabled for flexibility

### Content Types
- **Posts**: Technical blog posts about Docker, security, Git, and development tools
- **Pages**: Static content like About page
- **Shorts**: Brief content pieces

## Development Notes

- The site uses Hugo's Goldmark markdown renderer with unsafe HTML enabled
- Custom theme provides the site's unique styling and layout
- GitHub Actions workflow handles automatic deployment on push to main
- Content is primarily focused on technical tutorials and development insights