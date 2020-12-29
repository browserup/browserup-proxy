FROM ruby:2.5

RUN bundle config --global frozen 1

WORKDIR /

COPY ./client/ /ruby-client/

# Build ruby client gem, install locally
WORKDIR /ruby-client/
RUN gem build openapi_client.gemspec
RUN gem install ./openapi_client-1.0.0.gem

COPY . /ruby/
WORKDIR /ruby/

RUN gem install bundler
RUN bundle update --bundler
RUN bundle config --delete frozen
RUN bundle install

CMD ["bundle", "exec", "rspec", "--backtrace"]
