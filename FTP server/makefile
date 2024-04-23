server_dir = src/Server/*.java
utils_dir = src/Utils/*.java
messages_dir = src/Messages/*.java
bytecode_dir = ByteCode

build:
	@if [ ! -d ByteCode ];then mkdir ByteCode; fi
	@javac -d $(bytecode_dir) $(server_dir) $(messages_dir) $(utils_dir)
	@echo "Sources compiled successfully."

clean:
	@rm $(bytecode_dir)/*.class
	@rm -d $(bytecode_dir)
	@echo "Compilation results removed."
