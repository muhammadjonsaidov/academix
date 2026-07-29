# Railway SeaweedFS service. Wrapper image exists because plain `weed -s3` with no -s3.config
# rejects EVERY SigV4-signed request (the AWS SDK always signs when given credentials), so the
# backend genuinely cannot talk to an unconfigured SeaweedFS — see CLAUDE.md "Reality checks".
# Railway has no file mounts, so the identity config is generated at container start from env
# instead of baking secrets into the image.
#
# Railway service settings: Root Directory = infra/railway/seaweedfs, volume mounted at /data.
# Required env: SEAWEEDFS_ACCESS_KEY, SEAWEEDFS_SECRET_KEY (must match the backend's).
# Private-networking only — nothing browser-facing ever touches SeaweedFS (verified: all file
# bytes go through the backend's FileStorageService; no presigned URLs anywhere).
FROM chrislusf/seaweedfs:3.80

COPY entrypoint.sh /academix-entrypoint.sh
ENTRYPOINT ["/bin/sh", "/academix-entrypoint.sh"]
