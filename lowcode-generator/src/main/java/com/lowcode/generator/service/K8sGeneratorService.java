package com.lowcode.generator.service;

import com.lowcode.generator.entity.AppGenerateConfig;
import com.lowcode.generator.entity.GeneratedCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class K8sGeneratorService {

    public List<GeneratedCode> generateK8sFiles(AppGenerateConfig config) {
        List<GeneratedCode> codes = new ArrayList<>();

        String namespace = config.getNamespace() != null ? config.getNamespace() : "default";
        int replicas = config.getReplicas() != null ? config.getReplicas() : 2;
        String cpuRequest = config.getCpuRequest() != null ? config.getCpuRequest() : "200m";
        String memoryRequest = config.getMemoryRequest() != null ? config.getMemoryRequest() : "512Mi";
        String cpuLimit = config.getCpuLimit() != null ? config.getCpuLimit() : "500m";
        String memoryLimit = config.getMemoryLimit() != null ? config.getMemoryLimit() : "1Gi";

        codes.add(generateNamespace(config, namespace));
        codes.add(generateBackendDeployment(config, namespace, replicas, cpuRequest, memoryRequest, cpuLimit, memoryLimit));
        if (config.isIncludeFrontend()) {
            codes.add(generateFrontendDeployment(config, namespace, replicas));
        }
        codes.add(generateBackendService(config, namespace));
        if (config.isIncludeFrontend()) {
            codes.add(generateFrontendService(config, namespace));
        }
        codes.add(generateIngress(config, namespace));
        codes.add(generateConfigMap(config, namespace));
        codes.add(generateBackendHpa(config, namespace));
        if (config.isIncludeFrontend()) {
            codes.add(generateFrontendHpa(config, namespace));
        }
        codes.add(generateKustomization(config));
        codes.add(generateK8sReadme(config));

        return codes;
    }

    private GeneratedCode generateNamespace(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: v1\n");
        sb.append("kind: Namespace\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    environment: production\n");
        return new GeneratedCode("K8S_NAMESPACE", "namespace.yaml", "k8s/namespace.yaml", sb.toString());
    }

    private GeneratedCode generateBackendDeployment(AppGenerateConfig config, String namespace, int replicas,
                                            String cpuRequest, String memoryRequest,
                                            String cpuLimit, String memoryLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: apps/v1\n");
        sb.append("kind: Deployment\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: backend\n");
        sb.append("spec:\n");
        sb.append("  replicas: ").append(replicas).append("\n");
        sb.append("  selector:\n");
        sb.append("    matchLabels:\n");
        sb.append("      app: ").append(config.getAppCode()).append("\n");
        sb.append("      component: backend\n");
        sb.append("  template:\n");
        sb.append("    metadata:\n");
        sb.append("      labels:\n");
        sb.append("        app: ").append(config.getAppCode()).append("\n");
        sb.append("        component: backend\n");
        sb.append("    spec:\n");
        sb.append("      containers:\n");
        sb.append("        - name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("          image: ").append(config.getAppCode()).append(":").append(config.getVersion()).append("\n");
        sb.append("          imagePullPolicy: IfNotPresent\n");
        sb.append("          ports:\n");
        sb.append("            - containerPort: 8080\n");
        sb.append("              name: http\n");
        sb.append("              protocol: TCP\n");
        sb.append("          resources:\n");
        sb.append("            requests:\n");
        sb.append("              cpu: ").append(cpuRequest).append("\n");
        sb.append("              memory: ").append(memoryRequest).append("\n");
        sb.append("            limits:\n");
        sb.append("              cpu: ").append(cpuLimit).append("\n");
        sb.append("              memory: ").append(memoryLimit).append("\n");
        sb.append("          env:\n");
        sb.append("            - name: SPRING_PROFILES_ACTIVE\n");
        sb.append("              value: prod\n");
        sb.append("            - name: DB_HOST\n");
        sb.append("              valueFrom:\n");
        sb.append("                configMapKeyRef:\n");
        sb.append("                  name: ").append(config.getAppCode()).append("-config\n");
        sb.append("                  key: db.host\n");
        sb.append("            - name: DB_PORT\n");
        sb.append("              valueFrom:\n");
        sb.append("                configMapKeyRef:\n");
        sb.append("                  name: ").append(config.getAppCode()).append("-config\n");
        sb.append("                  key: db.port\n");
        sb.append("            - name: DB_NAME\n");
        sb.append("              valueFrom:\n");
        sb.append("                configMapKeyRef:\n");
        sb.append("                  name: ").append(config.getAppCode()).append("-config\n");
        sb.append("                  key: db.name\n");
        sb.append("            - name: REDIS_HOST\n");
        sb.append("              valueFrom:\n");
        sb.append("                configMapKeyRef:\n");
        sb.append("                  name: ").append(config.getAppCode()).append("-config\n");
        sb.append("                  key: redis.host\n");
        sb.append("          livenessProbe:\n");
        sb.append("            httpGet:\n");
        sb.append("              path: /actuator/health\n");
        sb.append("              port: 8080\n");
        sb.append("            initialDelaySeconds: 30\n");
        sb.append("            periodSeconds: 10\n");
        sb.append("          readinessProbe:\n");
        sb.append("            httpGet:\n");
        sb.append("              path: /actuator/health\n");
        sb.append("              port: 8080\n");
        sb.append("            initialDelaySeconds: 10\n");
        sb.append("            periodSeconds: 5\n");
        return new GeneratedCode("K8S_DEPLOYMENT_BACKEND", "deployment-backend.yaml", "k8s/deployment-backend.yaml", sb.toString());
    }

    private GeneratedCode generateFrontendDeployment(AppGenerateConfig config, String namespace, int replicas) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: apps/v1\n");
        sb.append("kind: Deployment\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-frontend\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: frontend\n");
        sb.append("spec:\n");
        sb.append("  replicas: ").append(replicas).append("\n");
        sb.append("  selector:\n");
        sb.append("    matchLabels:\n");
        sb.append("      app: ").append(config.getAppCode()).append("\n");
        sb.append("      component: frontend\n");
        sb.append("  template:\n");
        sb.append("    metadata:\n");
        sb.append("      labels:\n");
        sb.append("        app: ").append(config.getAppCode()).append("\n");
        sb.append("        component: frontend\n");
        sb.append("    spec:\n");
        sb.append("      containers:\n");
        sb.append("        - name: ").append(config.getAppCode()).append("-frontend\n");
        sb.append("          image: ").append(config.getAppCode()).append("-frontend:").append(config.getVersion()).append("\n");
        sb.append("          imagePullPolicy: IfNotPresent\n");
        sb.append("          ports:\n");
        sb.append("            - containerPort: 80\n");
        sb.append("              name: http\n");
        sb.append("              protocol: TCP\n");
        sb.append("          resources:\n");
        sb.append("            requests:\n");
        sb.append("              cpu: 100m\n");
        sb.append("              memory: 256Mi\n");
        sb.append("            limits:\n");
        sb.append("              cpu: 200m\n");
        sb.append("              memory: 512Mi\n");
        sb.append("          livenessProbe:\n");
        sb.append("            httpGet:\n");
        sb.append("              path: /\n");
        sb.append("              port: 80\n");
        sb.append("            initialDelaySeconds: 10\n");
        sb.append("            periodSeconds: 10\n");
        sb.append("          readinessProbe:\n");
        sb.append("            httpGet:\n");
        sb.append("              path: /\n");
        sb.append("              port: 80\n");
        sb.append("            initialDelaySeconds: 5\n");
        sb.append("            periodSeconds: 5\n");
        return new GeneratedCode("K8S_DEPLOYMENT_FRONTEND", "deployment-frontend.yaml", "k8s/deployment-frontend.yaml", sb.toString());
    }

    private GeneratedCode generateBackendService(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: v1\n");
        sb.append("kind: Service\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: backend\n");
        sb.append("spec:\n");
        sb.append("  type: ClusterIP\n");
        sb.append("  ports:\n");
        sb.append("    - port: 8080\n");
        sb.append("      targetPort: 8080\n");
        sb.append("      protocol: TCP\n");
        sb.append("      name: http\n");
        sb.append("  selector:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: backend\n");
        return new GeneratedCode("K8S_SERVICE_BACKEND", "service-backend.yaml", "k8s/service-backend.yaml", sb.toString());
    }

    private GeneratedCode generateFrontendService(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: v1\n");
        sb.append("kind: Service\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-frontend\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: frontend\n");
        sb.append("spec:\n");
        sb.append("  type: ClusterIP\n");
        sb.append("  ports:\n");
        sb.append("    - port: 80\n");
        sb.append("      targetPort: 80\n");
        sb.append("      protocol: TCP\n");
        sb.append("      name: http\n");
        sb.append("  selector:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("    component: frontend\n");
        return new GeneratedCode("K8S_SERVICE_FRONTEND", "service-frontend.yaml", "k8s/service-frontend.yaml", sb.toString());
    }

    private GeneratedCode generateIngress(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: networking.k8s.io/v1\n");
        sb.append("kind: Ingress\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-ingress\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("  annotations:\n");
        sb.append("    nginx.ingress.kubernetes.io/rewrite-target: /\n");
        sb.append("    nginx.ingress.kubernetes.io/ssl-redirect: \"false\"\n");
        sb.append("spec:\n");
        sb.append("  rules:\n");
        sb.append("    - host: ").append(config.getAppCode()).append(".example.com\n");
        sb.append("      http:\n");
        sb.append("        paths:\n");
        sb.append("          - path: /\n");
        sb.append("            pathType: Prefix\n");
        sb.append("            backend:\n");
        sb.append("              service:\n");
        sb.append("                name: ").append(config.getAppCode()).append("-frontend\n");
        sb.append("                port:\n");
        sb.append("                  number: 80\n");
        sb.append("          - path: /api\n");
        sb.append("            pathType: Prefix\n");
        sb.append("            backend:\n");
        sb.append("              service:\n");
        sb.append("                name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("                port:\n");
        sb.append("                  number: 8080\n");
        return new GeneratedCode("K8S_INGRESS", "ingress.yaml", "k8s/ingress.yaml", sb.toString());
    }

    private GeneratedCode generateConfigMap(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: v1\n");
        sb.append("kind: ConfigMap\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-config\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("data:\n");
        sb.append("  db.host: \"").append(config.getDbHost() != null ? config.getDbHost() : "mysql").append("\"\n");
        sb.append("  db.port: \"").append(config.getDbPort() != null ? config.getDbPort() : "3306").append("\"\n");
        sb.append("  db.name: \"").append(config.getDbName() != null ? config.getDbName() : config.getAppCode()).append("\"\n");
        sb.append("  redis.host: \"").append(config.getRedisHost() != null ? config.getRedisHost() : "redis").append("\"\n");
        sb.append("  redis.port: \"").append(config.getRedisPort() != null ? config.getRedisPort() : "6379").append("\"\n");
        return new GeneratedCode("K8S_CONFIGMAP", "configmap.yaml", "k8s/configmap.yaml", sb.toString());
    }

    private GeneratedCode generateBackendHpa(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: autoscaling/v2\n");
        sb.append("kind: HorizontalPodAutoscaler\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-backend-hpa\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("spec:\n");
        sb.append("  scaleTargetRef:\n");
        sb.append("    apiVersion: apps/v1\n");
        sb.append("    kind: Deployment\n");
        sb.append("    name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("  minReplicas: 2\n");
        sb.append("  maxReplicas: 10\n");
        sb.append("  metrics:\n");
        sb.append("    - type: Resource\n");
        sb.append("      resource:\n");
        sb.append("        name: cpu\n");
        sb.append("        target:\n");
        sb.append("          type: Utilization\n");
        sb.append("          averageUtilization: 70\n");
        sb.append("    - type: Resource\n");
        sb.append("      resource:\n");
        sb.append("        name: memory\n");
        sb.append("        target:\n");
        sb.append("          type: Utilization\n");
        sb.append("          averageUtilization: 80\n");
        return new GeneratedCode("K8S_HPA_BACKEND", "hpa-backend.yaml", "k8s/hpa-backend.yaml", sb.toString());
    }

    private GeneratedCode generateFrontendHpa(AppGenerateConfig config, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: autoscaling/v2\n");
        sb.append("kind: HorizontalPodAutoscaler\n");
        sb.append("metadata:\n");
        sb.append("  name: ").append(config.getAppCode()).append("-frontend-hpa\n");
        sb.append("  namespace: ").append(namespace).append("\n");
        sb.append("  labels:\n");
        sb.append("    app: ").append(config.getAppCode()).append("\n");
        sb.append("spec:\n");
        sb.append("  scaleTargetRef:\n");
        sb.append("    apiVersion: apps/v1\n");
        sb.append("    kind: Deployment\n");
        sb.append("    name: ").append(config.getAppCode()).append("-frontend\n");
        sb.append("  minReplicas: 2\n");
        sb.append("  maxReplicas: 10\n");
        sb.append("  metrics:\n");
        sb.append("    - type: Resource\n");
        sb.append("      resource:\n");
        sb.append("        name: cpu\n");
        sb.append("        target:\n");
        sb.append("          type: Utilization\n");
        sb.append("          averageUtilization: 60\n");
        sb.append("    - type: Resource\n");
        sb.append("      resource:\n");
        sb.append("        name: memory\n");
        sb.append("        target:\n");
        sb.append("          type: Utilization\n");
        sb.append("          averageUtilization: 70\n");
        return new GeneratedCode("K8S_HPA_FRONTEND", "hpa-frontend.yaml", "k8s/hpa-frontend.yaml", sb.toString());
    }

    private GeneratedCode generateKustomization(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: kustomize.config.k8s.io/v1beta1\n");
        sb.append("kind: Kustomization\n");
        sb.append("\n");
        sb.append("namespace: ").append(config.getNamespace() != null ? config.getNamespace() : "default").append("\n");
        sb.append("\n");
        sb.append("resources:\n");
        sb.append("  - namespace.yaml\n");
        sb.append("  - configmap.yaml\n");
        sb.append("  - deployment-backend.yaml\n");
        sb.append("  - service-backend.yaml\n");
        if (config.isIncludeFrontend()) {
            sb.append("  - deployment-frontend.yaml\n");
            sb.append("  - service-frontend.yaml\n");
        }
        sb.append("  - ingress.yaml\n");
        sb.append("  - hpa-backend.yaml\n");
        if (config.isIncludeFrontend()) {
            sb.append("  - hpa-frontend.yaml\n");
        }
        sb.append("\n");
        sb.append("commonLabels:\n");
        sb.append("  app: ").append(config.getAppCode()).append("\n");
        sb.append("  version: ").append(config.getVersion()).append("\n");
        return new GeneratedCode("K8S_KUSTOMIZATION", "kustomization.yaml", "k8s/kustomization.yaml", sb.toString());
    }

    private GeneratedCode generateK8sReadme(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Kubernetes 部署说明\n\n");
        sb.append("## 概述\n\n");
        sb.append("本目录包含 ").append(config.getAppName()).append(" 应用的 Kubernetes 部署配置文件。\n\n");
        sb.append("## 文件说明\n\n");
        sb.append("- `namespace.yaml`: 命名空间定义\n");
        sb.append("- `configmap.yaml`: 应用配置\n");
        sb.append("- `deployment.yaml`: 后端服务部署\n");
        sb.append("- `service.yaml`: 服务定义\n");
        sb.append("- `ingress.yaml`: 入口路由配置\n");
        sb.append("- `hpa.yaml`: 水平Pod自动扩缩容\n");
        sb.append("- `kustomization.yaml`: Kustomize 配置文件\n\n");
        sb.append("## 部署步骤\n\n");
        sb.append("### 方式一：使用 kubectl 直接部署\n\n");
        sb.append("```bash\n");
        sb.append("# 创建命名空间\n");
        sb.append("kubectl apply -f namespace.yaml\n\n");
        sb.append("# 部署应用\n");
        sb.append("kubectl apply -f configmap.yaml\n");
        sb.append("kubectl apply -f deployment.yaml\n");
        sb.append("kubectl apply -f service.yaml\n");
        sb.append("kubectl apply -f ingress.yaml\n");
        sb.append("kubectl apply -f hpa.yaml\n");
        sb.append("```\n\n");
        sb.append("### 方式二：使用 Kustomize 部署\n\n");
        sb.append("```bash\n");
        sb.append("kubectl apply -k .\n");
        sb.append("```\n\n");
        sb.append("## 验证部署\n\n");
        sb.append("```bash\n");
        sb.append("# 查看Pod状态\n");
        sb.append("kubectl get pods -n ").append(config.getNamespace() != null ? config.getNamespace() : "default").append("\n\n");
        sb.append("# 查看服务状态\n");
        sb.append("kubectl get svc -n ").append(config.getNamespace() != null ? config.getNamespace() : "default").append("\n\n");
        sb.append("# 查看Ingress\n");
        sb.append("kubectl get ingress -n ").append(config.getNamespace() != null ? config.getNamespace() : "default").append("\n");
        sb.append("```\n\n");
        sb.append("## 配置说明\n\n");
        sb.append("- 默认副本数：2\n");
        sb.append("- CPU 请求：200m，限制：500m\n");
        sb.append("- 内存请求：512Mi，限制：1Gi\n");
        sb.append("- 自动扩缩容：CPU 70% 触发扩容\n");
        return new GeneratedCode("K8S_README", "README.md", "k8s/README.md", sb.toString());
    }
}
