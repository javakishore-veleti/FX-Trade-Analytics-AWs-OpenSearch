export interface OpenSearchDeployment {
  id: number;
  cloudProvider: string;       // 'aws' | 'azure' | 'gcp' | 'local'
  provisionType: string;       // 'managed' | 'serverless' | 'local-docker'
  deploymentName: string;
  region: string;
  status: string;              // 'ACTIVE' | 'PROCESSING' | 'INACTIVE' | 'ERROR'
  endpoint?: string;           // canonical https URL from the AWS describe-* API
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

export interface DashboardTemplateResult {
  template: string;
  ok: boolean;
  message: string;
}

export interface DashboardInstallResult {
  deploymentId: number;
  endpoint: string;
  templatesAttempted: number;
  templatesSucceeded: number;
  results: DashboardTemplateResult[];
}
