# If SITE_SHA is set, use the specific SHA, else HEAD.
# Mount host folder to /home/juxt/.local/share/site/db to save persistent state
# build with:   sudo docker build -t juxt/site:latest .
# check image:  sudo docker image ls
# drop in:      sudo docker run -it juxt/site bash
# run with:     sudo docker run -p 2021:2021 -p 50505:50505 -d --name site juxt/site:latest
FROM clojure:openjdk-17-tools-deps
RUN apt-get update
RUN apt-get -y install wget git
WORKDIR /opt
RUN git clone https://github.com/juxt/site.git
WORKDIR /opt/site
ARG SITE_SHA
RUN if [ -z "$SITE_SHA" ] ; then echo Using Site master HEAD ; else git checkout -b custom $SITE_SHA ; fi

RUN addgroup juxt && adduser --system juxt && adduser juxt juxt
RUN chown -R juxt:juxt /opt/site && chmod u+w /opt/site
USER juxt
RUN mkdir -p $HOME/.config/site
RUN cp etc/config.edn $HOME/.config/site/config.edn
EXPOSE 50505
EXPOSE 2021
CMD ["/opt/site/bin/site-server"]
