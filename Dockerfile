ARG RUBY_VERSION=2.7.6-slim-bullseye
ARG JAVA_VERSION=11-slim-bullseye
ARG LEIHS_UI_VERSION=dev

# more build args in later stages:
# ARG WORKDIR=/leihs/mail
# ARG GIT_COMMIT_ID=unknown-commit-id
# ARG GIT_TREE_ID=unknown-tree-id

# === STAGE: BASE JAVA ========================================================================== #
FROM openjdk:${JAVA_VERSION} as leihs-base-java

# === STAGE: BASE RUBY ========================================================================== #
FROM ruby:${RUBY_VERSION} as leihs-base-ruby

# === STAGE: BUILD UBERJAR ====================================================================== #
FROM leihs-base-java as builder

ARG WORKDIR=/leihs/mail
WORKDIR "$WORKDIR"

# OS deps
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        git && \
    rm -rf /var/lib/apt/lists/*

# "merge" in ruby installation from offical image (matching debian version).
COPY --from=leihs-base-ruby /usr /usr/
RUN echo 'gem: --no-document' >> /usr/local/etc/gemrc
# smoke check
RUN ruby --version && echo "GEM: $(gem --version)" && bundle --version

# prepare clojure dependencies
# `clojure -X:deps prep`        -> Prepare all unprepped libs in the dep tree
# `clojure -T:build-leihs prep` -> hack, throws an error, but download deps before that ;)
ENV PATH=${WORKDIR}/shared-clj/clojure/bin:$PATH
COPY deps.edn .
COPY scripts/build.clj .
COPY shared-clj shared-clj/
RUN clojure -X:deps prep && \
    ( clojure -T:build-leihs prep || true )

# copy sources
COPY . .

# BUILD: see bin/build (`function build`) for the steps, leaving out those that are prepared (dependencies, …)
RUN ./bin/clj-uberjar
# NOTE: git info comes last, because the commit-id acts a cache-buster. therefore all expensive tasks need to be done before!
#       also, we dont run git inside the container (needs full repo inside), but instead supply needed info etc via build arg/env vars:
ARG GIT_COMMIT_ID=unknown-commit-id
ARG GIT_TREE_ID=unknown-tree-id
RUN BUILD_COMMIT_ID="${GIT_COMMIT_ID}" BUILD_TREE_ID="${GIT_TREE_ID}" ./bin/set-built-info
RUN jar uf target/leihs-mail.jar resources/built-info.yml

# === STAGE: PROD IMAGE ========================================================================= #
FROM leihs-base-java
ARG WORKDIR=/leihs/mail
WORKDIR "$WORKDIR"

COPY --from=builder ${WORKDIR}/target/leihs-mail.jar target/

# config
ENV HTTP_PORT=3250

# run
EXPOSE ${HTTP_PORT}
ENTRYPOINT [ "java", "-jar", "target/leihs-mail.jar", "run" ]