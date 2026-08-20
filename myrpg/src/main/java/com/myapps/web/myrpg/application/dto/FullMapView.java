package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 전체지도 뷰를 나타내는 뷰 모델 레코드.
 *
 * <p>모든 맵 노드를 바운딩박스 기준으로 격자에 배치한 결과를 담는다. {@code columns}와 {@code rows}는 격자의 최대 열/행 수를 나타낸다.
 *
 * @param cells 전체지도 격자 내 셀 목록 (전체 노드 포함)
 * @param columns 격자 열 수 (maxX - minX + 1)
 * @param rows 격자 행 수 (maxY - minY + 1)
 */
public record FullMapView(List<FullMapCell> cells, int columns, int rows) {}
