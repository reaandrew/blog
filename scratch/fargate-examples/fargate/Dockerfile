FROM alpine:3.9
MAINTAINER "email@andrewrea.co.uk"

RUN apk --no-cache add py2-pip freshclam jq curl git && \
    pip install awscli

RUN adduser -S worker
USER worker
WORKDIR /home/worker
COPY backup.sh .

CMD []
ENTRYPOINT ["./backup.sh"]
