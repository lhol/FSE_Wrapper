FROM ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y \
    build-essential cmake git openjdk-21-jdk dotnet-sdk-8.0 maven

WORKDIR /workspace
COPY . .

RUN cd native && mkdir build && cd build && cmake .. && cmake --build . --config Release
RUN cd java && mvn -B clean package
RUN cd Huff0.Net/src/Huff0.Net && dotnet build -c Release

FROM ubuntu:22.04
WORKDIR /app
COPY --from=build /workspace/native/build /app/native
COPY --from=build /workspace/java/target /app/java
COPY --from=build /workspace/Huff0.Net/src/Huff0.Net/bin/Release /app/dotnet
