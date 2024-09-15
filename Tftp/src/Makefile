# Variables
SRC_DIR = tftp
BIN_DIR = bin
SERVER_DIR = $(BIN_DIR)/server
JAVA_FILES = $(wildcard $(SRC_DIR)/*.java)
CLASS_FILES = $(patsubst $(SRC_DIR)/%.java, $(BIN_DIR)/%.class, $(JAVA_FILES))

# Default target
all: $(CLASS_FILES)

# Compile Java files into the bin directory
$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SRC_DIR)/*.java

# Clean target to remove compiled files
clean:
	rm -rf $(BIN_DIR)/*.class

# Run target to execute the TftpServer
run: all
	java -cp $(BIN_DIR) tftp.TftpServer

.PHONY: all clean run
