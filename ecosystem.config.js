module.exports = {
  apps: [
    {
      name: "trade-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-trade-service -Dspring-boot.run.arguments=--server.port=8080"
    },
    {
      name: "risk-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-risk-service -Dspring-boot.run.arguments=--server.port=8081"
    },
    {
      name: "indexer-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-opensearch-indexer -Dspring-boot.run.arguments=--server.port=8082"
    },
    {
      name: "admin-ui",
      script: "npm",
      args: "start",
      cwd: "portals/admin-portal"
    },
    {
      name: "customer-ui",
      script: "npm",
      args: "start",
      cwd: "portals/customer-portal"
    }
  ]
};