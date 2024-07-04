SRC_FILES = $(shell find $(SRC_DIR) -name '*.cpp')

jvmtiAgent.so: $(SRC_FILES)
	g++ -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux \
            -I $(JAVA_HOME)/../include -I $(JAVA_HOME)/../include/linux \
            -fPIC -shared -g -o $@ $^
