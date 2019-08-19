FROM alpine:3.9
MAINTAINER "andy.rea@valtech.com"

RUN apk --no-cache add py2-pip freshclam && \
    pip install awscli

RUN adduser -S worker
USER worker
WORKDIR /home/worker
COPY backup.sh .

CMD ["./backup.sh"]
