# DNS Lookup Tool

This tool provides basic DNS operations such as querying fully qualified domain names (FQDNs), tracing requests, changing the DNS server, and dumping results. It is designed to be simple and efficient for DNS troubleshooting tasks.

## Features

- **DNS Lookup**: Query DNS records for any domain.
- **DNS Trace**: Enable or disable request tracing.
- **Change DNS Server**: Set a specific DNS server for lookups.
- **Dump Results**: Dumps cached recent DNS lookup results.
- **Quit**: Exit the tool.

## Commands

The following commands are available in the DNS Lookup Tool:

### 1. `lookup fqdn [type]`

Performs a DNS lookup for the specified domain.  
- `fqdn`: The fully qualified domain name to look up (e.g., `google.com`).
- `type`: (Optional) The type of DNS record to retrieve (e.g., `A`, `MX`, `CNAME`). If not provided, it defaults to `A` records.

- ### 2. `trace on|off`

Enables or disables tracing of DNS request processing. This can be useful for debugging DNS issues.  
- `on`: Enables DNS tracing.
- `off`: Disables DNS tracing.

- ### 3. `server IP`

Changes the DNS server to a custom server by specifying its IP address. 

- ### 4. `dump`

Dumps cached recent DNS lookup results.

- ### 5. `quit`

Exits the DNS Lookup Tool.

## Commands
go to the file directory,make clean, make, then java -jar DNSLookupService.jar rootserver(example: 198.41.0.4)








