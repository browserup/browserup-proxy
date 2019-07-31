require 'openapi_client'

describe OpenapiClient do
  it 'connects to api' do
    api_instance = OpenapiClient::DefaultApi.new
    api_instance.api_client.config.host = 'localhost:35629'
    port = 8081
    url_pattern = '^.*$'

    begin
      p api_instance.entries(port, url_pattern)
    rescue OpenapiClient::ApiError => e
      puts "Exception when calling DefaultApi->entries: #{e}"
    end
  end
end