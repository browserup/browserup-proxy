FROM node:8.16.0-alpine

USER root

WORKDIR /

COPY ./client/ /client/

# Build javascript client, install locally
WORKDIR /client/
RUN rm -rf node_modules/
RUN npm install
RUN npm link
RUN npm link /client
RUN npm run build

COPY . /javascript/
WORKDIR /javascript/

CMD ["node", "test/javascript_test.js"]