# DNS Lookup Tool

This tool provides basic DNS operations such as querying fully qualified domain names (FQDNs), tracing requests, changing the DNS server, and dumping results. It is designed to be simple and efficient for DNS troubleshooting tasks.

## Features

- **DNS Lookup**: Query DNS records for any domain.
- **DNS Trace**: Enable or disable request tracing.
- **Change DNS Server**: Set a specific DNS server for lookups.
- **Dump Results**: Save the output of your commands to a file.
- **Quit**: Exit the tool.

## Commands

The following commands are available in the DNS Lookup Tool:

### 1. `lookup fqdn [type]`

Performs a DNS lookup for the specified domain.  
- `fqdn`: The fully qualified domain name to look up (e.g., `google.com`).
- `type`: (Optional) The type of DNS record to retrieve (e.g., `A`, `MX`, `CNAME`). If not provided, it defaults to `A` records.

**Example:**
```bash
lookup google.com A

### 1. `lookup fqdn [type]`

Performs a DNS lookup for the specified domain.  
- `fqdn`: The fully qualified domain name to look up (e.g., `google.com`).
- `type`: (Optional) The type of DNS record to retrieve (e.g., `A`, `MX`, `CNAME`). If not provided, it defaults to `A` records.

**Example:**
```bash
lookup google.com A




