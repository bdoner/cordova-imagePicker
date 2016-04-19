//
//  AssetCell.h
//
//  Created by ELC on 2/15/11.
//  Copyright 2011 ELC Technologies. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "ELCAsset.h"

@protocol ELCAssetCellDelegate;

@interface ELCAssetCell : UITableViewCell

@property (nonatomic, strong) NSObject<ELCAssetCellDelegate> * delegate;

- (void)setAssets:(NSArray *)assets;

@end

@protocol ELCAssetCellDelegate <NSObject>

-(NSInteger)getSelectedIndexForAsset:(ELCAsset*)asset sender:(ELCAssetCell*)cell;
-(void)didTapCell:(ELCAsset*)asset sender:(ELCAssetCell*)cell;

@end
