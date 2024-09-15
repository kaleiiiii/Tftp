# Variables
BIN_DIR = bin
JAVA_FILES = $(wildcard *.java)

# Default target
all: $(BIN_DIR) $(JAVA_FILES:.java=.class)

# Create bin directory
$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# Compile Java files into the bin directory
%.class: %.java | $(BIN_DIR)
	javac -d $(BIN_DIR) $(JAVA_FILES)

# Clean target to remove all files in the bin directory
clean:
	rm -rf $(BIN_DIR)/*

# Run target to execute the TftpServer
run: all
	java -cp $(BIN_DIR) TftpServer

.PHONY: all clean run
