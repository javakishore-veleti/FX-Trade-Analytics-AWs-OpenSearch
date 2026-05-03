export interface OpenSearchDeployment {
  id: number;
  cloudProvider: string;       // 'aws' | 'azure' | 'gcp' | 'local'
  provisionType: string;       // 'managed' | 'serverless' | 'local-docker'
  deploymentName: string;
  region: string;
  status: string;              // 'ACTIVE' | 'PROCESSING' | 'INACTIVE' | 'ERROR'
  configJson?: string;
  createdOn: string;
  updatedOn: string;
  syncedOn?: string;
  dashboardsUrl?: string;
  awsConsoleUrl?: string;
}

export interface OpenSearchSyncResult {
  regionsScanned: number;
  deploymentsDiscovered: number;
  updated: number;
  markedInactive: number;
  deployments: OpenSearchDeployment[];
  errors: string[];
}
