FROM alpine:3.9

RUN apk --no-cache add py2-pip freshclam && \
    pip install awscli

COPY backup_github_org.sh /

RUN adduser -S worker
USER worker
WORKDIR /home/worker

CMD ["/backup_github_org.sh"]
