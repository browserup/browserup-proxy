require 'openapi_client'

describe OpenapiClient do
  it 'connects to api' do
    proxy_rest_host = ENV["PROXY_REST_HOST"]
    proxy_rest_port = ENV["PROXY_REST_PORT"]
    proxy_port = ENV["PROXY_PORT"]

    p "Using the following env variables:"
    p "PROXY_REST_HOST = #{proxy_rest_host}"
    p "PROXY_REST_PORT = #{proxy_rest_port}"
    p "PROXY_PORT = #{proxy_port}"

    api_instance = OpenapiClient::DefaultApi.new
    api_instance.api_client.config.host = "#{proxy_rest_host}:#{proxy_rest_port}"
    port = proxy_port
    url_pattern = '^.*$'

    begin
      entries_response = api_instance.entries(port, url_pattern).to_json
      p "Got the following entries in the response: #{entries_response}"
    rescue OpenapiClient::ApiError => e
      puts "Exception when calling DefaultApi->entries: #{e}"
      raise
    end
  end
end