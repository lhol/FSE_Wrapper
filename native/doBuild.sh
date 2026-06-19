export JAVA_HOME="C:\Users\LarsH\.jdks\temurin-25.0.2"
cd native
mkdir build && cd build
cmake ..
cmake --build . --config Release
