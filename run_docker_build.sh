docker buildx create --use
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t yourname/huff0-fse:latest \
  .
