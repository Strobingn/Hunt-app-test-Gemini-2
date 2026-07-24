# Hunt Align - AI & Machine Learning Heavy Roadmap

- [x] **1. On-Device LiDAR Point Cloud Classification & Terrain Feature Extractor**
  - Extract Slope Gradient, Aspect Angle, Topographic Curvature (Saddles, Ridges, Draws, Creeks).
  - Classify 3D point cloud points into Ground, Canopy, Edge, and Funnel zones (`TerrainFeatureExtractor.kt`).

- [x] **2. ML Wildlife Corridor & Least-Cost Movement Path Calculator**
  - Compute animal movement cost surfaces based on slope energy expenditure and canopy cover density.
  - Generate predictive game trail polylines on the interactive canvas (`CorridorPathfinder.kt`).

- [x] **3. AI Solar & Micro-Thermal Scent Plume Vector Simulator**
  - Simulate thermal wind vector flow (thermal updrafts / downdrafts) across LAZ terrain based on time-of-day, solar azimuth, and slope aspect.
  - Draw live dynamic scent dispersion cone overlays on the canvas around waypoints (`ThermalScentSimulator.kt`).

- [x] **4. AI Trail Camera Image & Wildlife Pattern Analyzer**
  - Integrated multimodal AI photo analyzer (Buck, Doe, Elk, Predator detection) correlated with LiDAR elevation waypoints.
  - Provides species frequency & activity timing heatmaps (`TrailCamAiAnalyzer.kt` & `AiTrailCamDialog.kt`).

- [x] **5. Predictive Stand & Camera Score Optimizer Dialog & Canvas Heatmap**
  - Highlighting high-probability stand locations (Score 0-100) based on AI terrain funnels, thermals, and vegetation density (`MlControlPanelBar.kt` & `AiTerrainDialog.kt`).
