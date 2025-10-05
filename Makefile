.PHONY: all clean build test package run-gui run-cli help docs

# Config
JAVA_VERSION = 17
SRC_DIR = src/main/java
RESOURCES_DIR = src/main/resources
TEST_DIR = test/java
BUILD_DIR = build
BIN_DIR = $(BUILD_DIR)/classes
TEST_BIN_DIR = $(BUILD_DIR)/test-classes
DIST_DIR = dist
LIB_DIR = lib
JAR_NAME = ImageMetadataManager.jar
MAIN_CLASS = com.imagemeta.Main

# compiler flags
JAVAC_FLAGS = --release $(JAVA_VERSION) -Xlint:all -Xlint:-this-escape
CLASSPATH = $(BIN_DIR):$(LIB_DIR)/*
TEST_CLASSPATH = $(TEST_BIN_DIR):$(BIN_DIR):$(LIB_DIR)/*:lib/junit-platform-console-standalone-1.10.1.jar

# Source files
JAVA_SOURCES = $(shell find $(SRC_DIR) -name "*.java")
TEST_SOURCES = $(shell find $(TEST_DIR) -name "*.java" 2>/dev/null)

all: build
release: clean package
everything: clean package docs

help:
	@echo "ImageMetadataManager Build System"
	@echo "=================================="
	@echo "Available targets:"
	@echo "  all        - Clean and build"
	@echo "  clean      - Remove build artifacts"
	@echo "  build      - Compile source code"
	@echo "  test       - Run all tests"
	@echo "  package    - Create JAR"
	@echo "  run-gui    - Run GUI"
	@echo "  run-cli    - Run CLI"
	@echo "  docs       - Generate JavaDoc"

clean:
	@echo "🧹 Cleaning..."
	@rm -rf $(BUILD_DIR) $(DIST_DIR)
	@echo "✅ Clean complete"

$(BIN_DIR):
	@mkdir -p $(BIN_DIR)

build: $(BIN_DIR)
	@echo "🔨 Compiling..."
	@javac $(JAVAC_FLAGS) -cp "$(LIB_DIR)/*" -d $(BIN_DIR) $(JAVA_SOURCES)
	@echo "📦 Copying resources..."
	@cp -R $(RESOURCES_DIR)/* $(BIN_DIR)/ 2>/dev/null || true
	@echo "✅ Build complete"

$(TEST_BIN_DIR):
	@mkdir -p $(TEST_BIN_DIR)

test: build $(TEST_BIN_DIR)
	@echo "🧪 Running tests..."
	@if [ -n "$(TEST_SOURCES)" ]; then \
		javac $(JAVAC_FLAGS) -cp "$(TEST_CLASSPATH)" -d $(TEST_BIN_DIR) $(TEST_SOURCES); \
		java -jar lib/junit-platform-console-standalone-1.10.1.jar \
		    execute \
			--class-path $(TEST_CLASSPATH) \
			--scan-class-path \
			--reports-dir=$(BUILD_DIR)/test-reports; \
		echo "✅ Tests complete"; \
	else \
		echo "⚠️  No tests found"; \
	fi

package: test
	@echo "📦 Creating package..."
	@mkdir -p $(DIST_DIR)
	@jar --create \
		--file $(DIST_DIR)/$(JAR_NAME) \
		--main-class $(MAIN_CLASS) \
		-C $(BIN_DIR) .
	@echo "✅ Package: $(DIST_DIR)/$(JAR_NAME)"

package-no-test: build
	@$(MAKE) package -o test

run-gui: build
	@echo "🚀 Launching GUI..."
	@_JAVA_AWT_WM_NONREPARENTING=1 java -cp "$(CLASSPATH)" $(MAIN_CLASS) --gui

run-cli: build
	@echo "🚀 Launching CLI..."
	@java -cp "$(CLASSPATH)" $(MAIN_CLASS) --cli --help

docs:
	@echo "📚 Generating docs..."
	@mkdir -p $(BUILD_DIR)/docs
	@javadoc -d $(BUILD_DIR)/docs \
		-sourcepath $(SRC_DIR) \
		-subpackages com.imagemeta \
		-classpath "$(LIB_DIR)/*" \
		-link https://docs.oracle.com/en/java/javase/17/docs/api/ \
		-tag "apiNote:a:API Note:" \
		-tag "implSpec:a:Implementation Requirements:" \
		-tag "implNote:a:Implementation Note:"
	@echo "✅ Docs: $(BUILD_DIR)/docs"

.DEFAULT_GOAL := help
