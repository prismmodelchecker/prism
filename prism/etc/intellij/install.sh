#!/bin/bash

# Path logic
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Check for force flag
FORCE=false
for arg in "$@"; do
  if [ "$arg" == "--force" ] || [ "$arg" == "-f" ]; then
    FORCE=true
  fi
done

# Check for symlink flag
SYMLINK=false
for arg in "$@"; do
  if [ "$arg" == "--symlink" ] || [ "$arg" == "-s" ]; then
    SYMLINK=true
  fi
done

copy_file() {
    local src="$1"
    local dest="$2"
	local display_path=${dest#"$PROJECT_ROOT/"}
    if [ -f "$dest" ] && [ "$FORCE" = false ]; then
        echo "⚠️  Skipping $display_path (already exists). Use -f to overwrite."
    else
	    if [ "$SYMLINK" = false ]; then
            cp "$src" "$dest" && echo "✅ Installed $display_path"
		else
		    rm "$dest" && ln -s "$src" "$dest" && echo "✅ Installed $display_path (symlink)"
    	fi
    fi
}

echo "🚀 Setting up IntelliJ configuration for PRISM..."

# Ensure directories exist
mkdir -p "$PROJECT_ROOT/.idea/libraries"
mkdir -p "$PROJECT_ROOT/.idea/runConfigurations"

# Execute copies
echo "Project root: $PROJECT_ROOT"
copy_file "$SCRIPT_DIR/prism.iml" "$PROJECT_ROOT/prism.iml"
copy_file "$SCRIPT_DIR/modules.xml" "$PROJECT_ROOT/.idea/modules.xml"
copy_file "$SCRIPT_DIR/lib.xml" "$PROJECT_ROOT/.idea/libraries/lib.xml"
copy_file "$SCRIPT_DIR/PrismCL.xml" "$PROJECT_ROOT/.idea/runConfigurations/PrismCL.xml"

echo -e "Done. Now open the project in IntelliJ IDEA."
