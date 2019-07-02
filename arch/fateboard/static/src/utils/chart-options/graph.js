export default {
  // title: {
  //   text: 'Graph 简单示例'
  // },
  tooltip: {},
  // animationDurationUpdate: 1500,
  // animationEasingUpdate: 'quinticInOut',
  series: [
    {
      type: 'graph',
      layout: 'none',
      roam: false, // 鼠标滚轮缩放
      label: {
        // normal: {
        //   show: true
        // }
        show: true,
        color: '#333',
        borderWidth: 1,
        borderRadius: 2,
        borderColor: '#333',
        // backgroundColor: '#984455',
        padding: 5,
        lineHeight: 20
      },
      symbol: 'roundRect',
      symbolSize: [60, 20],
      symbolOffset: [0, 0],
      edgeSymbol: ['circle', 'arrow'],
      edgeSymbolSize: [6, 10],
      // edgeLabel: {
      //   normal: {
      //     textStyle: {
      //       fontSize: 20
      //     }
      //   }
      // },
      data: [],
      links: [],
      itemStyle: {
        color: 'transparent'
      },
      lineStyle: {
        normal: {
          opacity: 0.9,
          width: 1,
          curveness: 0
        }
      }
    }
  ]
}