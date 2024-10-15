
# DNS Lookup Service Commands

This tool provides various commands to perform DNS lookups and trace DNS queries through different servers. Below is a list of valid commands and their usage.

## Commands

### 1. `lookup fqdn [type]`

- **Description**: Perform a DNS query for a fully qualified domain name (FQDN).
- **Parameters**:
  - `fqdn`: The fully qualified domain name (e.g., `www.google.com`).
  - `type` (optional): The type of DNS record to query, such as `A` (IPv4 address), `AAAA` (IPv6 address), `MX` (Mail Exchange). If omitted, the default record type is queried (usually `A`).
- **Example**:
  ```bash
  lookup www.google.com A

