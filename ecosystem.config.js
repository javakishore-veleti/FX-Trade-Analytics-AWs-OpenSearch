module.exports = {
  apps: [
    {
      name: "trade-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-trade-service -Dspring-boot.run.arguments=--server.port=9080"
    },
    {
      name: "risk-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-risk-service -Dspring-boot.run.arguments=--server.port=9081"
    },
    {
      name: "indexer-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-opensearch-indexer -Dspring-boot.run.arguments=--server.port=9082"
    },
    {
      name: "masterdata-service",
      script: "mvn",
      args: "spring-boot:run -pl middleware/fx-masterdata-service -Dspring-boot.run.arguments=--server.port=9083"
    },
    {
      name: "admin-ui",
      script: "npm",
      args: "run start:admin",
      cwd: "portals"
    },
    {
      name: "customer-ui",
      script: "npm",
      args: "run start:customer",
      cwd: "portals"
    }
  ]
};