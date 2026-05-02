# Architecture Overview

## 🌍 Multi-Region Setup

- us-east-1 → Trade generation
- us-west-2 → Processing
- ap-south-1 → Analytics

## 🔄 Data Flow

Trade Simulator → Kafka → OpenSearch Indexer → OpenSearch (region-specific domains)

## ⭐ Key Feature

OpenSearch Cross-Region UI allows querying all regions from a single dashboard without data replication.
