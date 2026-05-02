# FX-Trade-Analytics-AWs-OpenSearch
This project demonstrates how to build a global FX trade analytics platform using AWS OpenSearch cross-region UI.

With the newly launched cross-region data access feature, you can query and visualize data across multiple AWS regions from a single OpenSearch UI — without replicating data or switching endpoints.
https://aws.amazon.com/about-aws/whats-new/2026/05/opensearch-ui-cross-region-data-access-domains/

## What This Application Does
- Multi-region FX trade data ingestion
- Event-driven architecture using Kafka
- Risk calculation pipeline
- OpenSearch indexing across regions
- Single dashboard querying multiple regions

## Architecture
Trade Generator → Kafka → Risk Service → OpenSearch (multi-region)

Single OpenSearch UI → Query all regions

## Why This Matters
Traditionally, multi-region analytics required:
- Data replication
- Complex routing
- Multiple dashboards

With OpenSearch cross-region UI:
- Query data in-place
- Reduce cost
- Maintain regional isolation

## Dashboards
- TraderBook Analytics
- Currency Pair Trends
- Time-of-Day FX Activity
- Risk Exposure
- Discount Impact Analysis

## Tech Stack
- Spring Boot 3.x
- Apache Kafka
- PostgreSQL
- AWS OpenSearch
- Docker

## Getting Started
./devops/local/docker-all-up.sh


