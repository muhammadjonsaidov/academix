# Railway RabbitMQ service. Deliberately the PLAIN image, not -management: the management UI's
# Erlang processes add ~100MB RSS, which matters under the trial's 0.5GB/service cap. Inspect
# queues via `rabbitmqctl` in Railway's service shell instead (rabbitmqctl list_queues etc.).
#
# Railway service settings: Root Directory = infra/railway/rabbitmq, volume at /var/lib/rabbitmq.
# Required env: RABBITMQ_DEFAULT_USER, RABBITMQ_DEFAULT_PASS — MUST be set: the built-in
# guest/guest user only accepts loopback connections, so over Railway private networking the
# backend cannot authenticate as guest at all (see application.yml's rabbitmq comment).
FROM rabbitmq:4.3.2-alpine

COPY academix.conf /etc/rabbitmq/conf.d/90-academix.conf
