FROM ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y \
    build-essential cmake git openjdk-21-jdk maven

RUN apt-get install -y dotnet-sdk-8.0 || \
    (apt-get install -y wget && \
     wget https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb -O /tmp/packages-microsoft-prod.deb && \
     dpkg -i /tmp/packages-microsoft-prod.deb && \
     apt-get update && apt-get install -y dotnet-sdk-8.0)

WORKDIR /workspace
COPY . .

RUN cmake -B native/build -S native && \
    cmake --build native/build --config Release

RUN ./mvnw -B clean package -DskipTests

RUN dotnet build csharp/Huff0.net/src/Huff0.Net.csproj -c Release && \
    dotnet build csharp/Fse.net/src/Fse.Net.csproj -c Release

FROM ubuntu:22.04
WORKDIR /app
COPY --from=build /workspace/native/build /app/native
COPY --from=build /workspace/target /app/java
COPY --from=build /workspace/csharp/Huff0.net/src/bin/Release /app/dotnet/Huff0.net
COPY --from=build /workspace/csharp/Fse.net/src/bin/Release /app/dotnet/Fse.net
