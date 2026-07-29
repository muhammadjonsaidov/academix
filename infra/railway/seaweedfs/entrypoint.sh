#!/bin/sh
# Generates the S3 identity config from env at startup (Railway has no file mounts), then starts
# SeaweedFS in the same single-binary server mode as infra/docker-compose.yml.
set -eu

: "${SEAWEEDFS_ACCESS_KEY:?SEAWEEDFS_ACCESS_KEY is required}"
: "${SEAWEEDFS_SECRET_KEY:?SEAWEEDFS_SECRET_KEY is required}"

mkdir -p /etc/seaweedfs

cat > /etc/seaweedfs/s3-config.json <<EOF
{
  "identities": [
    {
      "name": "academix",
      "credentials": [
        {
          "accessKey": "${SEAWEEDFS_ACCESS_KEY}",
          "secretKey": "${SEAWEEDFS_SECRET_KEY}"
        }
      ],
      "actions": ["Admin", "Read", "Write"]
    }
  ]
}
EOF

# -master.volumeSizeLimitMB keeps individual volume files small so the Railway volume (a few GB)
# never hits the "no writable volumes" state from one oversized preallocated volume.
exec weed server \
  -s3 \
  -s3.port=8333 \
  -s3.config=/etc/seaweedfs/s3-config.json \
  -dir=/data \
  -master.volumeSizeLimitMB=256 \
  -volume.max=8
