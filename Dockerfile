# If SITE_SHA is set, use the specific SHA, else HEAD.
# Mount host folder to /home/juxt/.local/share/site/db to save persistent state
# build with:  sudo docker build -t juxt/site:latest .
# drop in:     sudo docker run -it juxt/site bash
# run with:    sudo docker run -p 2021:2021 -p 50505:50505 -d juxt/site:latest
FROM clojure:openjdk-17-tools-deps
RUN apt-get update
RUN apt-get -y install git
WORKDIR /opt
RUN git clone https://github.com/juxt/site.git
WORKDIR /opt/site
ARG SITE_SHA
RUN if [ -z "$SITE_SHA" ] ; then echo Using Site master HEAD ; else git checkout -b custom $SITE_SHA ; fi

RUN addgroup juxt && adduser --system juxt && adduser juxt juxt
RUN chown -R juxt:juxt /opt/site && chmod u+w /opt/site
USER juxt
EXPOSE 50505
EXPOSE 2021
RUN clojure -P
RUN clj -M:dev -e "(println :loading-deps-repl)"
CMD [ "clojure", \
      "-J-Djava.awt.headless=true", \
      "-J-XX:-OmitStackTraceInFastThrow", \
      "-J-Dclojure.server.site={:port,50505,:accept,juxt.site.alpha.repl-server/repl,:address,\"0.0.0.0\"}", \
      "-J-Dlogback.configurationFile=$HOME/.config/site/logback.xml", \
      "-J-Dsite.config=/opt/site/etc/config.edn", \
      "-J-Xms256m", \
      "-J-Xmx2400m", \
      "-Mprod", \
      "-m", \
      "juxt.site.alpha.main" ]
